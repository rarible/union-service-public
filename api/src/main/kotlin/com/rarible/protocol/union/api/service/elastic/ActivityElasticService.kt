package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySearchFilterDto
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
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentActivityId
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityElasticService(
    private val filterConverter: ActivityFilterConverter,
    private val router: BlockchainRouter<ActivityService>,
    private val activityRepository: ActivityRepository,
    private val esActivityOptimizedSearchService: EsActivityOptimizedSearchService,
) : ActivityQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val start = System.currentTimeMillis()

        val effectiveCursor = cursor ?: continuation
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getAllActivities(), blockchains={}", blockchains)
            return ActivitiesDto()
        }

        val filter = filterConverter.convertGetAllActivities(type, enabledBlockchains, bidCurrencies, effectiveCursor)
        val result = executeSearch(filter, size, sort)

        logger.info(
            "Response for ES getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={}) ({}ms)",
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
        val start = System.currentTimeMillis()
        val filteredCollections = collection.filter { router.isBlockchainEnabled(it.blockchain) }
        if (filteredCollections.isEmpty()) {
            return ActivitiesDto()
        }
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByCollection(
            type,
            filteredCollections.map { it.fullId() },
            bidCurrencies,
            effectiveCursor
        )

        val result = executeSearch(filter, size, sort)

        logger.info(
            "Response for ES getActivitiesByCollection(type={}, collection={}, cursor={}, size={}, sort={}): " +
                "Slice(size={}, cursor={}) ({}ms)",
            type, collection, effectiveCursor, size, sort, result.activities.size, result.cursor, latency(start)
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
        val start = System.currentTimeMillis()
        if (!router.isBlockchainEnabled(itemId.blockchain)) {
            return ActivitiesDto()
        }
        val effectiveCursor = cursor ?: continuation
        val filter = filterConverter.convertGetActivitiesByItem(type, itemId.fullId(), bidCurrencies, effectiveCursor)

        val result = executeSearch(filter, size, sort)

        logger.info(
            "Response for ES getActivitiesByItem(type={}, itemId={} cursor={}, size={}, sort={}): " +
                "Slice(size={}, cursor={}) ({}ms)",
            type, itemId, effectiveCursor, size, sort, result.activities.size, result.cursor, latency(start)
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
        val start = System.currentTimeMillis()
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
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

        val result = executeSearch(filter, size, sort)

        logger.info(
            "Response for ES getActivitiesByUser(type={}, users={}, cursor={}, size={}, sort={}):" +
                " Slice(size={}, cursor={}) ({}ms)",
            type, user, effectiveCursor, size, sort, result.activities.size, result.cursor, latency(start)
        )

        return result
    }

    suspend fun searchActivities(
        filter: ActivitySearchFilterDto,
        size: Int?,
        sort: ActivitySortDto?,
        cursor: String?,
    ): ActivitiesDto {
        val start = System.currentTimeMillis()
        val esFilter = filterConverter.convertDtoFilter(
            filter = filter,
            activeBlockchains = router.getEnabledBlockchains(filter.blockchains).toSet()
        )

        val result = executeSearch(esFilter, size, sort)

        logger.info(
            "Response for ES searchActivities(filter={}, cursor={}, size={}, sort={}):" +
                " Slice(size={}, cursor={}) ({}ms)",
            filter, cursor, size, sort, result.activities.size, result.cursor, latency(start)
        )
        return result
    }

    private suspend fun executeSearch(
        filter: ElasticActivityFilter,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val queryResult = esActivityOptimizedSearchService.search(filter, convertSort(sort), size)
        val activities = getActivities(queryResult.entities)

        return ActivitiesDto(
            continuation = null,
            cursor = queryResult.continuation,
            activities = activities
        )
    }

    private suspend fun getActivities(esActivities: List<EsActivity>): List<ActivityDto> {
        if (esActivities.isEmpty()) return emptyList()
        val enrichmentActivities = activityRepository.getAll(esActivities.map {
            EnrichmentActivityId.of(it.activityId)
        }).associateBy { it.id }
        return esActivities.mapNotNull {
            enrichmentActivities[EnrichmentActivityId.of(it.activityId)]?.let { activity ->
                EnrichmentActivityDtoConverter.convert(source = activity, cursor = it.fromActivity().toString())
            }
        }
    }

    private fun convertSort(sort: ActivitySortDto?): EsActivitySort {
        val latestFirst = when (sort) {
            ActivitySortDto.LATEST_FIRST, null -> true
            ActivitySortDto.EARLIEST_FIRST -> false
        }
        return EsActivitySort(latestFirst)
    }

    private fun latency(start: Long): Long {
        return System.currentTimeMillis() - start
    }
}
