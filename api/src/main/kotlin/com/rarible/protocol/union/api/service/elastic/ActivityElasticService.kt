package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.mapAsync
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
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityElasticService(
    private val filterConverter: ActivityFilterConverter,
    private val esActivityRepository: EsActivityRepository,
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
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
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
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
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
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
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
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
        val activities = getActivities(queryResult.activities)
        return ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )
    }

    private suspend fun getActivities(activities: List<EsActivity>): List<ActivityDto> {
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

        val results = evaluatedBlockchains.mapAsync { blockchain ->
            val ids = blockchainMap[blockchain]
            if (!ids.isNullOrEmpty()) {
                router.getService(blockchain).getActivitiesByIds(ids)
            } else null
        }.filterNotNull()

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

    private fun convertSort(sort: ActivitySortDto?): EsActivitySort {
        val latestFirst = when (sort) {
            ActivitySortDto.LATEST_FIRST, null -> true
            ActivitySortDto.EARLIEST_FIRST -> false
        }
        return EsActivitySort(latestFirst)
    }
}
