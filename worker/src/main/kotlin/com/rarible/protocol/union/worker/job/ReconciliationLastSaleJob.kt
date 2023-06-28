package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.ItemLastSaleConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.worker.config.WorkerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationLastSaleJob(
    private val activityServiceRouter: BlockchainRouter<ActivityService>,
    private val itemEventService: EnrichmentItemEventService,
    private val itemService: EnrichmentItemService,
    properties: WorkerProperties
) : AbstractBlockchainBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    override suspend fun handleBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Sell Activities from {}: [{}]", blockchain.name, continuation)
        val page = activityServiceRouter.getService(blockchain).getAllActivities(
            types = listOf(ActivityTypeDto.SELL),
            continuation = continuation,
            size = config.activityBatchSize,
            sort = ActivitySortDto.LATEST_FIRST
        )

        val activities = page.entities

        if (activities.isEmpty()) {
            logger.info(
                "RECONCILIATION LAST SALE STATE FOR {}: There is no more Sell Activities for continuation {}",
                blockchain.name, continuation
            )
            return null
        }

        coroutineScope {
            activities.asFlow()
                .map { async { safeUpdate(it) } }
                .buffer(config.threadCount)
                .map { it.await() }
                .collect()
        }

        logger.info(
            "RECONCILIATION LAST SALE STATE FOR {}: {} Activities updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun safeUpdate(activity: UnionActivity) {

        try {
            // Originally, it should not happen
            if (ItemLastSaleConverter.convert(activity) == null) return
            // If item doesn't exist, skip activity
            val item = itemService.fetchOrNull(ShortItemId(activity.itemId()!!)) ?: return

            itemEventService.onActivity(activity, item, null, config.notificationEnabled)
        } catch (e: Exception) {
            logger.error("Unable to reconcile Sell Activity {} : {}", activity, e.message, e)
        }
    }
}