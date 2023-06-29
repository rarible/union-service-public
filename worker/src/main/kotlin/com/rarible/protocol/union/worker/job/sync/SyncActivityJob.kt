package com.rarible.protocol.union.worker.job.sync

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SyncActivityJob(
    private val activityServiceRouter: BlockchainRouter<ActivityService>,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val esActivityRepository: EsActivityRepository,
    private val converter: EsActivityConverter,
    esRateLimiter: EsRateLimiter
) : AbstractSyncJob<UnionActivity, EnrichmentActivity, SyncActivityJobParam>(
    "Activity",
    SyncActivityJobParam::class.java,
    esRateLimiter
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val batchSize = 50

    override suspend fun getNext(param: SyncActivityJobParam, state: String?): Slice<UnionActivity> {
        val service = activityServiceRouter.getService(param.blockchain)
        return when {
            param.reverted -> service.getAllRevertedActivitiesSync(state, batchSize, param.sort, param.type)
            else -> service.getAllActivitiesSync(state, batchSize, param.sort, param.type)
        }
    }

    override suspend fun updateDb(
        param: SyncActivityJobParam,
        unionEntities: List<UnionActivity>
    ): List<EnrichmentActivity> {
        // We should NOT catch exceptions here, our SYNC job should not skip entities
        return unionEntities.mapAsync { activity ->
            enrichmentActivityService.update(activity)
        }
    }

    override suspend fun updateEs(
        param: SyncActivityJobParam,
        enrichmentEntities: List<EnrichmentActivity>,
        unionEntities: List<UnionActivity>
    ) {
        val toDelete = unionEntities.filter { it.reverted == true }.map { it.id }.toSet()
        val toSave = enrichmentEntities.filter { it.id.toDto() !in toDelete }
            .map { EnrichmentActivityDtoConverter.convert(source = it, reverted = false) }

        esActivityRepository.bulk(
            entitiesToSave = converter.batchConvert(toSave),
            idsToDelete = toDelete.map { it.fullId() },
            indexName = param.esIndex,
            refreshPolicy = WriteRequest.RefreshPolicy.NONE,
        )
    }

    override suspend fun notify(param: SyncActivityJobParam, enrichmentEntities: List<EnrichmentActivity>) {
        // TODO Not needed for Activities?
    }

    override fun isDone(param: SyncActivityJobParam, batch: Slice<UnionActivity>): Boolean {
        if (param.to == null || batch.entities.isEmpty()) return false

        val last = batch.entities.last().lastUpdatedAt

        return when (param.sort) {
            SyncSortDto.DB_UPDATE_DESC -> param.to.isAfter(last)
            SyncSortDto.DB_UPDATE_ASC -> param.to.isBefore(last)
        }
    }
}

data class SyncActivityJobParam(
    override val blockchain: BlockchainDto,
    override val scope: SyncScope,
    override val esIndex: String? = null,
    val type: SyncTypeDto,
    // With task state (which is 'continuation') and 'sort/to' combination we can reindex any time range
    val sort: SyncSortDto = SyncSortDto.DB_UPDATE_DESC,
    val to: Instant? = null,
    val reverted: Boolean = false
) : AbstractSyncJobParam()