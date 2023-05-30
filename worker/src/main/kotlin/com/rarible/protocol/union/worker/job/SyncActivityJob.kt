package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
// TODO ATM we have running migration, refactor this job when it done
@Deprecated("Refactor it using AbstractSyncJob as base class")
class SyncActivityJob(
    private val activityServiceRouter: BlockchainRouter<ActivityService>,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val activityRepository: ActivityRepository,
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val batchSize = 50

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Activities from {}: [{}]", blockchain.name, continuation)
        val page = activityServiceRouter.getService(blockchain).getAllActivitiesSync(
            continuation = continuation,
            size = batchSize,
            sort = SyncSortDto.DB_UPDATE_DESC,
            type = null,
        )

        val activities = page.entities

        if (activities.isEmpty()) {
            logger.info(
                "SYNC ACTIVITY STATE FOR {}: There is no more Activities for continuation {}",
                blockchain, continuation
            )
            return null
        }

        activities.forEach {
            if (!it.isValid()) {
                logger.info("Ignoring activity $it as it is not valid")
                return@forEach
            }
            activityRepository.save(enrichmentActivityService.enrich(it))
        }

        logger.info(
            "SYNC ACTIVITY STATE FOR {}: {} Activities updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }
}