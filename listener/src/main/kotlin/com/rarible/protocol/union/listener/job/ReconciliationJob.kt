package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.service.OrderServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationJob(
    private val orderServiceRouter: OrderServiceRouter,
    private val orderEventService: EnrichmentOrderEventService,
    properties: UnionListenerProperties
) {

    private val logger = LoggerFactory.getLogger(ReconciliationTaskHandler::class.java)

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
            PlatformDto.ALL,
            null,
            lastUpdateContinuation,
            config.orderBatchSize
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
            orders.asFlow().map { async { safeUpdate(it) } }
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
            orderEventService.updateOrder(order)
        } catch (e: Exception) {
            logger.error("Unable to reconcile order {} : {}", e.message, e)
        }
    }
}