package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.elastic.ActivityElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityApiMergeService
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityQueryService
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ActivitySourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val activityApiMergeService: ActivityApiMergeService,
    private val activityElasticService: ActivityElasticService,
) {

    suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getAllActivities(type, blockchains, continuation, cursor, size, sort)
    }

    suspend fun getAllActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto {
        return activityApiMergeService.getAllActivitiesSync(blockchain, continuation, size, sort, type)
    }

    suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<String>,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getActivitiesByCollection(
            type, collection, continuation, cursor, size, sort
        )
    }

    suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
    }

    suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getActivitiesByUser(
            type, user, blockchains, from, to, continuation, cursor, size, sort
        )
    }

    private fun getQuerySource(searchEngine: SearchEngineDto?, sort: ActivitySortDto?): ActivityQueryService {
        if (searchEngine != null) {
            return when (searchEngine) {
                SearchEngineDto.V1 -> activityElasticService
                SearchEngineDto.LEGACY -> activityApiMergeService
            }
        }

        return if (featureFlagsProperties.enableActivityQueriesToElasticSearch) {
            if (featureFlagsProperties.enableActivityAscQueriesWithApiMerge) {
                if (sort == ActivitySortDto.EARLIEST_FIRST) {
                    activityApiMergeService
                } else {
                    activityElasticService
                }
            } else {
                activityElasticService
            }
        } else {
            activityApiMergeService
        }
    }
}
