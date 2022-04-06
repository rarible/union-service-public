package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationOrderJob(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val orderEventService: EnrichmentOrderEventService,
    properties: UnionListenerProperties
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Orders from {}: [{}]", blockchain.name, continuation)
        val page = orderServiceRouter.getService(blockchain).getOrdersAll(
            continuation,
            config.orderBatchSize,
            OrderSortDto.LAST_UPDATE_DESC,
            listOf(OrderStatusDto.ACTIVE)
        )

        val orders = page.entities

        if (orders.isEmpty()) {
            logger.info(
                "RECONCILIATION ORDER STATE FOR {}: There is no more Orders for continuation {}",
                blockchain.name, continuation
            )
            return null
        }

        coroutineScope {
            orders.asFlow()
                .filter {
                    val isPublic = it.taker == null
                    if (!isPublic) logger.info("Ignore private order [{}]", it.id)
                    isPublic
                }
                .map { async { safeUpdate(it) } }
                .buffer(config.threadCount)
                .map { it.await() }
                .collect()
        }

        logger.info(
            "RECONCILIATION ORDER STATE FOR {}: {} Orders updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun safeUpdate(order: OrderDto) {
        try {
            // TODO: better to send notification if order has changes
            orderEventService.updateOrder(order, config.notificationEnabled)
        } catch (e: Exception) {
            logger.error("Unable to reconcile order {} : {}", e.message, e)
        }
    }
}
