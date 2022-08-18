package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityApiMergeService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class ActivityReindexService(
    private val activityApiMergeService: ActivityApiMergeService,
    private val esActivityRepository: EsActivityRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val converter: EsActivityConverter,
) {
    companion object {
        val logger by Logger()
    }

    fun reindex(
        blockchain: BlockchainDto,
        type: ActivityTypeDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        val counter = searchTaskMetricFactory.createReindexActivityCounter(blockchain, type)

        // TODO read values from config
        val size = when (blockchain) {
            BlockchainDto.IMMUTABLEX -> 200 // Max size allowed by IMX
            else -> PageSize.ACTIVITY.max
        }

        return flow {
            var continuation = cursor
            do {
                val res = activityApiMergeService.getAllActivities(
                    listOf(type),
                    listOf(blockchain),
                    continuation,
                    continuation,
                    size,
                    ActivitySortDto.LATEST_FIRST
                )

                val before = nowMillis()
                logger.info("Saving ${res.activities.size} activities, continuation: $continuation")
                val savedActivities = esActivityRepository.saveAll(
                    converter.batchConvert(res.activities),
                    index,
                    refreshPolicy = WriteRequest.RefreshPolicy.NONE,
                )
                logger.info("Saved ${res.activities.size} activities, continuation: $continuation, took ${nowMillis().toEpochMilli() - before.toEpochMilli()}ms")
                continuation = res.cursor
                counter.increment(savedActivities.size)
                emit(res.cursor ?: "")
            } while (res.cursor.isNullOrEmpty().not())
        }
    }
}