package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityApiMergeService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.RateLimiter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component

@Component
class ActivityReindexService(
    private val activityApiMergeService: ActivityApiMergeService,
    private val esActivityRepository: EsActivityRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val converter: EsActivityConverter,
    private val rateLimiter: RateLimiter,
) {
    companion object {
        val logger by Logger()
    }

    fun reindex(
        blockchain: BlockchainDto,
        type: SyncTypeDto,
        index: String?,
        cursor: String? = null,
        from: Long? = null,
        to: Long? = null,
    ): Flow<String> {
        val counter = searchTaskMetricFactory.createReindexActivityCounter(blockchain, type)
        val size = limit(blockchain)
        return flow {
            var continuation = cursor
            do {
                rateLimiter.waitIfNecessary(size)

// TODO consider calling specific blockchain ActivityService.getAllActivities(), will be slightly faster
                val res = activityApiMergeService.getAllActivitiesSync(
                    type = type,
                    blockchain = blockchain,
                    continuation = continuation,
                    size = size,
                    sort = SyncSortDto.DB_UPDATE_DESC
                )

                val before = nowMillis()
                logger.info("Saving ${res.activities.size} activities, continuation: $continuation")
                val savedActivities = esActivityRepository.saveAll(
                    converter.batchConvert(res.activities),
                    index,
                    refreshPolicy = WriteRequest.RefreshPolicy.NONE,
                )
                counter.increment(savedActivities.size)
                logger.info("Saved ${res.activities.size} activities, continuation: $continuation, took ${nowMillis().toEpochMilli() - before.toEpochMilli()}ms")

                continuation = TimePeriodContinuationHelper.adjustContinuation(res.cursor, from, to)
                emit(continuation ?: "")
            } while (continuation.isNullOrEmpty().not())
        }
    }

    fun removeReverted(
        blockchain: BlockchainDto,
        type: SyncTypeDto,
        cursor: String?,
    ): Flow<String> {
        val size = limit(blockchain)
        return flow {
            var continuation = cursor
            do {
                val result = activityApiMergeService.getAllRevertedActivitiesSync(
                    blockchain = blockchain,
                    continuation = continuation,
                    size = size,
                    sort = SyncSortDto.DB_UPDATE_DESC,
                    type = type
                )
                logger.info("Delete reverted ${result.activities.size} activities, continuation: $continuation")
                if (result.activities.isNotEmpty()) {
                    val deleted = esActivityRepository.deleteAll(result.activities.map { it.id.toString() })
                    logger.info("Deleted $deleted reverted activities")
                }
                continuation = result.continuation
                emit(continuation ?: "")
            } while (continuation.isNullOrEmpty().not())
        }
    }

    private fun limit(blockchain: BlockchainDto): Int {
        // TODO read values from config
        return when (blockchain) {
            BlockchainDto.IMMUTABLEX -> 200 // Max size allowed by IMX
            else -> PageSize.ACTIVITY.max
        }
    }
}