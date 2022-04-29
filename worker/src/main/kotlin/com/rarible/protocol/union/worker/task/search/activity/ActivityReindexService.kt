package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class ActivityReindexService(
    private val activityClient: ActivityControllerApi,
    private val esActivityRepository: EsActivityRepository,
    private val converter: EsActivityConverter,
    private val meterRegistry: MeterRegistry
) {

    fun reindex(
        blockchain: BlockchainDto,
        activityType: ActivityTypeDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        return flow {
            val res = activityClient.getAllActivities(
                listOf(activityType),
                listOf(blockchain),
                cursor,
                cursor,
                ActivityTask.PAGE_SIZE,
                ActivitySortDto.EARLIEST_FIRST
            ).awaitFirst()

            val savedActivities = esActivityRepository.saveAll(
                res.activities.mapNotNull(converter::convert),
                index
            )

            meterRegistry.gauge(
                "union.search.reindex.activity",
                listOf(
                    Tag.of("blockchain", blockchain.name),
                    Tag.of("activityType", activityType.name)
                ),
                savedActivities.size
            )

            emit(res.cursor ?: "")
        }
    }
}