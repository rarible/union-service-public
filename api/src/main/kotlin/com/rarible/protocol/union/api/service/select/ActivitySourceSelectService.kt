package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.ActivityService
import com.rarible.protocol.union.api.service.api.ActivityApiService
import com.rarible.protocol.union.api.service.elastic.ActivityElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivitySourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val activityApiMergeService: ActivityApiService,
    private val activityElasticService: ActivityElasticService,
) : ActivityService {

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        return when (featureFlagsProperties.enableActivityQueriesToElasticSearch) {
            true -> activityElasticService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            else -> activityApiMergeService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
        }
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        return when (featureFlagsProperties.enableActivityQueriesToElasticSearch) {
            true -> activityElasticService.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
            else -> activityApiMergeService.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
        }
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        return when (featureFlagsProperties.enableActivityQueriesToElasticSearch) {
            true -> activityElasticService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            else -> activityApiMergeService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
        }
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        return when (featureFlagsProperties.enableActivityQueriesToElasticSearch) {
            true -> activityElasticService.getActivitiesByUser(type, user, blockchains, from, to, continuation, cursor, size, sort)
            else -> activityApiMergeService.getActivitiesByUser(type, user, blockchains, from, to, continuation, cursor, size, sort)
        }
    }
}
