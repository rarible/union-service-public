package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class ActivityReindexService(
    private val activityClient: ActivityControllerApi,
    private val esActivityRepository: EsActivityRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory
) {
    fun reindex(
        blockchain: BlockchainDto,
        type: ActivityTypeDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        val counter = searchTaskMetricFactory.createReindexActivityCounter(blockchain, type)

        return flow {
            do {
                val res = activityClient.getAllActivities(
                    listOf(type),
                    listOf(blockchain),
                    cursor,
                    cursor,
                    PageSize.ACTIVITY.max,
                    ActivitySortDto.EARLIEST_FIRST,
                    false
                ).awaitFirst()

                val savedActivities = esActivityRepository.saveAll(
                    res.activities.mapNotNull(EsActivityConverter::convert),
                    index
                )
                counter.increment(savedActivities.size)
                emit(res.cursor ?: "")
            } while (res.cursor != null)
        }
    }
}
