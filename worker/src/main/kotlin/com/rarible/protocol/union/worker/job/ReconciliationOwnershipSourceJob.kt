package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
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
class ReconciliationOwnershipSourceJob(
    private val activityServiceRouter: BlockchainRouter<ActivityService>,
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val ownershipService: EnrichmentOwnershipService,
    properties: WorkerProperties,
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Mint/Transfer Activities from {}: [{}]", blockchain.name, continuation)
        val page = activityServiceRouter.getService(blockchain).getAllActivities(
            types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.TRANSFER),
            continuation = continuation,
            size = config.activityBatchSize,
            sort = ActivitySortDto.LATEST_FIRST
        )

        val activities = page.entities

        if (activities.isEmpty()) {
            logger.info(
                "RECONCILIATION OWNERSHIP SOURCE STATE FOR {}: There is no more Mint/Transfer Activities for continuation {}",
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
            "RECONCILIATION OWNERSHIP STATE FOR {}: {} Activities updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun safeUpdate(activity: UnionActivity) {
        try {
            if (activity.source() == null || activity.ownershipId() == null) return
            // If ownership doesn't exist, skip activity
            val ownership = ownershipService.fetchOrNull(ShortOwnershipId(activity.ownershipId()!!)) ?: return

            ownershipEventService.onActivity(activity, ownership, null, config.notificationEnabled)
        } catch (e: Exception) {
            logger.error("Unable to reconcile Mint/Transfer Activity {} : {}", activity, e.message, e)
        }
    }
}