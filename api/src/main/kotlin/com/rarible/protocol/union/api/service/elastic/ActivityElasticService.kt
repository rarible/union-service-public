package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.ActivityQueryService
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.model.ActivitySort
import com.rarible.protocol.union.search.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.search.core.service.query.ActivityElasticQueryService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityElasticService(
    private val filterConverter: ActivityFilterConverter,
    private val queryService: ActivityElasticQueryService,
    private val router: BlockchainRouter<ActivityService>,
) : ActivityQueryService {

    companion object {
        val logger by Logger()
    }

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetAllActivities(type, blockchains, effectiveCursor)
        val activitySort = ActivitySort(latestFirst = (sort == ActivitySortDto.LATEST_FIRST))
        val queryResult = queryService.query(filter, activitySort, size ?: 50)
        val activities = getActivities(queryResult.activities)
        return ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByCollection(type, collection, effectiveCursor)
        val activitySort = ActivitySort(latestFirst = (sort == ActivitySortDto.LATEST_FIRST))
        val queryResult = queryService.query(filter, activitySort, size ?: 50)
        val activities = getActivities(queryResult.activities)
        return ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByItem(type, itemId, effectiveCursor)
        val activitySort = ActivitySort(latestFirst = (sort == ActivitySortDto.LATEST_FIRST))
        val queryResult = queryService.query(filter, activitySort, size ?: 50)
        val activities = getActivities(queryResult.activities)
        return ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )
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
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByUser(type, user, blockchains, from, to, effectiveCursor)
        val activitySort = ActivitySort(latestFirst = (sort == ActivitySortDto.LATEST_FIRST))
        val queryResult = queryService.query(filter, activitySort, size ?: 50)
        val activities = getActivities(queryResult.activities)
        return ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )
    }

    private suspend fun getActivities(activities: List<ElasticActivity>): List<ActivityDto> {
        if (activities.isEmpty()) return emptyList()

        val positionMap = mutableMapOf<String, Int>()
        val blockchainMap = mutableMapOf<BlockchainDto, MutableList<TypedActivityId>>()
        activities.forEachIndexed { index, activity ->
            positionMap[activity.activityId] = index
            blockchainMap.compute(activity.blockchain) { _, v ->
                if (v == null) {
                    mutableListOf(TypedActivityId(activity.activityId, activity.type))
                } else {
                    v.add(TypedActivityId(activity.activityId, activity.type))
                    v
                }
            }
        }

        val evaluatedBlockchains = router.getEnabledBlockchains(blockchainMap.keys)

        val results = coroutineScope {
            evaluatedBlockchains.mapNotNull { blockchain ->
                val ids = blockchainMap[blockchain]
                if (!ids.isNullOrEmpty()) {
                    async {
                        router.getService(blockchain).getActivitiesByIds(ids)
                    }
                } else null
            }.awaitAll()
        }

        val mergedResult = arrayOfNulls<ActivityDto>(activities.size)
        results.forEach {
            it.forEach { activity ->
                val index = positionMap[activity.id.toString()]
                if (index != null) {
                    mergedResult[index] = activity
                } else {
                    logger.warn("Couldn't find position of ${activity.id} in result array")
                }
            }
        }

        return mergedResult.filterNotNull()
    }
}
