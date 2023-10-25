package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.elastic.ActivityElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySearchFilterDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UnionAddress
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
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getAllActivities(
            type,
            blockchains,
            bidCurrencies,
            continuation,
            cursor,
            size,
            sort
        )
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
        collection: List<CollectionIdDto>,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getActivitiesByCollection(
            type, collection, bidCurrencies, continuation, cursor, size, sort
        )
    }

    suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: ItemIdDto,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getActivitiesByItem(
            type,
            itemId,
            bidCurrencies,
            continuation,
            cursor,
            size,
            sort
        )
    }

    suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<UnionAddress>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<CurrencyIdDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ActivitiesDto {
        return getQuerySource(searchEngine, sort).getActivitiesByUser(
            type, user, blockchains, bidCurrencies, from, to, continuation, cursor, size, sort
        )
    }

    suspend fun search(
        filter: ActivitySearchFilterDto,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
    ): ActivitiesDto {
        return activityElasticService.searchActivities(
            filter = filter,
            size = size,
            sort = sort,
            cursor = cursor
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
            if (featureFlagsProperties.enableActivityAscQueriesWithApiMerge &&
                !featureFlagsProperties.enableOptimizedSearchForActivities) {
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
