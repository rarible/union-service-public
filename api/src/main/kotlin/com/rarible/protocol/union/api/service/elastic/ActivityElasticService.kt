package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.flatMapAsync
import com.rarible.protocol.union.api.dto.applyCursor
import com.rarible.protocol.union.api.metrics.ElasticMetricsFactory
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentActivityId
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@Service
@CaptureSpan(type = SpanType.APP)
class ActivityElasticService(
    private val filterConverter: ActivityFilterConverter,
    private val esActivityRepository: EsActivityRepository,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val router: BlockchainRouter<ActivityService>,
    private val activityRepository: ActivityRepository,
    private val featureFlagsProperties: FeatureFlagsProperties,
    elasticMetricsFactory: ElasticMetricsFactory,
) : ActivityQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val missingIdsMetrics = elasticMetricsFactory.missingActivitiesCounters

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val requestId = ThreadLocalRandom.current().nextLong()
        val start = System.currentTimeMillis()

        val effectiveCursor = cursor ?: continuation
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getAllActivities(), blockchains={}", blockchains)
            return ActivitiesDto()
        }
        val filter = filterConverter.convertGetAllActivities(type, enabledBlockchains, bidCurrencies, effectiveCursor)
        logger.info("[$requestId] Built filter: $filter")
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
        logger.info("[$requestId] Get query result: ${queryResult.activities.size}, ${latency(start)}")

        val activities = getActivities(queryResult.activities)
        val result = ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )

        logger.info(
            "[{}] Response for ES getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={}, latency={})",
            requestId,
            type,
            blockchains,
            continuation,
            size,
            sort,
            result.activities.size,
            result.continuation,
            result.cursor,
            latency(start)
        )
        return result
    }

    override suspend fun getAllActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto {
        throw UnsupportedOperationException("Operation is not supported for Elastic Search")
    }

    override suspend fun getAllRevertedActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto {
        throw UnsupportedOperationException("Operation is not supported for Elastic Search")
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<CollectionIdDto>,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val filteredCollections = collection.filter { router.isBlockchainEnabled(it.blockchain) }
        if (filteredCollections.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getActivitiesByCollection(), collection={}", collection)
            return ActivitiesDto()
        }
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByCollection(
            type,
            filteredCollections.map { it.fullId() },
            bidCurrencies,
            effectiveCursor
        )
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)

        val activities = getActivities(queryResult.activities)

        val result = ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )

        logger.info(
            "Response for ES getActivitiesByCollection(type={}, collection={}, continuation={}, size={}, sort={}): " +
                "Slice(size={}, continuation={}) ",
            type, collection, continuation, size, sort, result.activities.size, result.continuation
        )

        return result
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: ItemIdDto,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        if (!router.isBlockchainEnabled(itemId.blockchain)) {
            logger.info("Unable to find enabled blockchains for getActivitiesByItem(), item={}", itemId)
            return ActivitiesDto()
        }
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByItem(type, itemId.fullId(), bidCurrencies, effectiveCursor)
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
        val activities = getActivities(queryResult.activities)

        val result = ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )

        logger.info(
            "Response for ES getActivitiesByItem(type={}, itemId={} continuation={}, size={}, sort={}): " +
                "Slice(size={}, continuation={}) ",
            type, itemId, continuation, size, sort, result.activities.size, result.continuation
        )
        return result
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<UnionAddress>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<CurrencyIdDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info(
                "Unable to find enabled blockchains for getActivitiesByUser() where user={} and blockchains={}",
                user, blockchains
            )
            return ActivitiesDto()
        }

        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByUser(
            type,
            user.map { it.fullId() },
            enabledBlockchains,
            bidCurrencies,
            from,
            to,
            effectiveCursor
        )
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)
        val activities = getActivities(queryResult.activities)

        val result = ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )

        logger.info(
            "Response for ES getActivitiesByUser(type={}, users={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={})",
            type, user, continuation, size, sort,
            result.activities.size, result.continuation, result.cursor
        )

        return result
    }

    private suspend fun getActivities(esActivities: List<EsActivity>): List<ActivityDto> {
        if (esActivities.isEmpty()) return emptyList()
        if (featureFlagsProperties.enableMongoActivityRead) {
            val enrichmentActivities = activityRepository.getAll(esActivities.map {
                EnrichmentActivityId.of(it.activityId)
            }).associateBy { it.id }
            return esActivities.mapNotNull {
                enrichmentActivities[EnrichmentActivityId.of(it.activityId)]?.let { activity ->
                    EnrichmentActivityDtoConverter.convert(source = activity, cursor = it.fromActivity().toString())
                }
            }
        }
        val mapping = hashMapOf<BlockchainDto, MutableList<TypedActivityId>>()

        esActivities.forEach { activity ->
            mapping
                .computeIfAbsent(activity.blockchain) { ArrayList(esActivities.size) }
                .add(TypedActivityId(IdParser.parseActivityId(activity.activityId).value, activity.type))
        }
        val activities = mapping.flatMapAsync { (blockchain, ids) ->
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                val response = router.getService(blockchain).getActivitiesByIds(ids)
                checkMissingIds(blockchain, ids, response)
                response
            } else emptyList()
        }.let { enrichmentActivityService.enrichDeprecated(it) }

        val activitiesIdMapping = activities.associateBy { it.id.fullId() }
        return esActivities.mapNotNull { esActivity ->
            activitiesIdMapping[esActivity.activityId].applyCursor(esActivity.fromActivity().toString())
        }
    }

    private fun convertSort(sort: ActivitySortDto?): EsActivitySort {
        val latestFirst = when (sort) {
            ActivitySortDto.LATEST_FIRST, null -> true
            ActivitySortDto.EARLIEST_FIRST -> false
        }
        return EsActivitySort(latestFirst)
    }

    private fun checkMissingIds(
        blockchain: BlockchainDto,
        ids: List<TypedActivityId>,
        response: List<UnionActivity>
    ) {
        val foundIds = mutableSetOf<String>()
        response.mapTo(foundIds) { it.id.value }
        val missingIds = mutableListOf<String>()
        for (id in ids) {
            if (!foundIds.contains(id.id)) {
                missingIds.add(id.id)
            }
        }
        if (missingIds.isNotEmpty()) {
            logger.error("Ids found in ES missing in $blockchain: $missingIds")
            missingIdsMetrics[blockchain]!!.increment(missingIds.size.toDouble())
        }
    }

    private fun latency(start: Long): String {
        return "${System.currentTimeMillis() - start} ms"
    }
}
