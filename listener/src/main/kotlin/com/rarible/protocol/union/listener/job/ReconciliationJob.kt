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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationJob(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val orderEventService: EnrichmentOrderEventService,
    properties: UnionListenerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    fun reconcile(continuation: String?, blockchain: BlockchainDto): Flow<String> {
        return flow {
            var next = continuation
            do {
                next = reconcileOrders(next, blockchain)
                if (next != null) {
                    emit(next!!)
                }
            } while (next != null)
        }
    }

    suspend fun reconcileOrders(lastUpdateContinuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Orders from {}: [{}]", blockchain.name, lastUpdateContinuation)
        val page = orderServiceRouter.getService(blockchain).getOrdersAll(
            lastUpdateContinuation,
            config.orderBatchSize,
            OrderSortDto.LAST_UPDATE_DESC,
            listOf(OrderStatusDto.ACTIVE)
        )

        val orders = page.entities

        if (orders.isEmpty()) {
            logger.info(
                "RECONCILIATION STATE FOR {}: There is no more Orders for continuation {}, aborting reconciliation",
                blockchain.name, lastUpdateContinuation
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
            "RECONCILIATION STATE FOR {}: {} Orders updated, next continuation is [{}]",
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
