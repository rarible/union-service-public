package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
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
import com.rarible.protocol.union.core.model.EsActivityLite
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.APP)
class ActivityElasticService(
    private val filterConverter: ActivityFilterConverter,
    private val esActivityRepository: EsActivityRepository,
    private val router: BlockchainRouter<ActivityService>,
) : ActivityQueryService {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        logger.info("getAllActivities() from ElasticSearch")
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetAllActivities(type, blockchains, effectiveCursor)
        logger.info("Built filter: $filter")
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
        logger.info("Query result: $queryResult")
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
        logger.debug("Query result: $queryResult")
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

    private suspend fun getActivities(esActivities: List<EsActivityLite>): List<ActivityDto> {
        if (esActivities.isEmpty()) return emptyList()
        val mapping = hashMapOf<BlockchainDto, MutableList<TypedActivityId>>()

        esActivities.forEach { activity ->
            mapping
                .computeIfAbsent(activity.blockchain) { ArrayList(esActivities.size) }
                .add(TypedActivityId(IdParser.parseActivityId(activity.activityId).value, activity.type))
        }
        val activities = mapping.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) router.getService(blockchain).getActivitiesByIds(ids) else emptyList()
        }.flatten()

        val activitiesIdMapping = activities.associateBy { it.id.fullId() }
        return esActivities.mapNotNull { esActivity ->
            activitiesIdMapping[esActivity.activityId]
        }
    }

    private fun convertSort(sort: ActivitySortDto?): EsActivitySort {
        val latestFirst = when (sort) {
            ActivitySortDto.LATEST_FIRST, null -> true
            ActivitySortDto.EARLIEST_FIRST -> false
        }
        return EsActivitySort(latestFirst)
    }
}
