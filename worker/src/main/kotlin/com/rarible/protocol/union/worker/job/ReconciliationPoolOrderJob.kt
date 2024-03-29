package com.rarible.protocol.union.worker.job

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderEventService
import com.rarible.protocol.union.worker.config.WorkerProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationPoolOrderJob(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val orderEventService: EnrichmentOrderEventService,
    properties: WorkerProperties,
) : AbstractBlockchainBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    // One hand - we want to reduce amount of http requests,
    // On other hand - each order can produce a lot of events, so let's set small batch size here
    private val batchSize = 8

    override suspend fun handleBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching pool Orders from {}: [{}]", blockchain.name, continuation)
        val page = orderServiceRouter.getService(blockchain).getAmmOrdersAll(
            listOf(OrderStatusDto.ACTIVE),
            continuation,
            batchSize
        )

        if (page.entities.isEmpty()) {
            logger.info(
                "RECONCILIATION POOL ORDER STATE FOR {}: There is no more Orders for continuation {}",
                blockchain.name, continuation
            )
            return null
        }

        val orders = page.entities.filter { it.isPoolOrder() } // Just for case

        // It will be better to do NOT handle it in parallel, because we can get same items for different pool (1155)
        orders.forEach { order ->
            val itemIds = orderServiceRouter.fetchAllBySlices(order.id.blockchain) { service, continuation ->
                service.getAmmOrderItemIds(order.id.value, continuation, 1000)
            }
            logger.info("Found {} Items for pool Order", itemIds.size)
            itemIds.chunked(config.threadCount).forEach { chunk ->
                chunk.mapAsync { itemId -> safeUpdate(order, itemId) }
            }
        }

        logger.info(
            "RECONCILIATION POOL ORDER STATE FOR {}: {} Orders updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun safeUpdate(order: UnionOrder, itemId: ItemIdDto) {
        try {
            orderEventService.updatePoolOrderPerItem(
                order, itemId,
                PoolItemAction.INCLUDED,
                offchainEventMark("enrichment-in"),
                config.notificationEnabled
            )
        } catch (e: Exception) {
            logger.error("Unable to reconcile pool Order {} : {}", order.id, e.message, e)
        }
    }
}
