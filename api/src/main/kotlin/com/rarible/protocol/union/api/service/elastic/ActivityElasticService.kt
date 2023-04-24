package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.flatMapAsync
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
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.APP)
class ActivityElasticService(
    private val filterConverter: ActivityFilterConverter,
    private val esActivityRepository: EsActivityRepository,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val router: BlockchainRouter<ActivityService>,
    elasticMetricsFactory: ElasticMetricsFactory,
    private val ff: FeatureFlagsProperties
) : ActivityQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val missingIdsMetrics = elasticMetricsFactory.missingActivitiesCounters

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val effectiveCursor = cursor ?: continuation
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getAllActivities(), blockchains={}", blockchains)
            return ActivitiesDto()
        }
        val filter = filterConverter.convertGetAllActivities(type, enabledBlockchains, effectiveCursor)
        logger.info("Built filter: $filter")
        val queryResult = esActivityRepository.search(filter, convertSort(sort), size)

        val activities = getActivities(queryResult.activities)
        val result = ActivitiesDto(
            continuation = null,
            cursor = queryResult.cursor,
            activities = activities
        )

        logger.info(
            "Response for ES getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={})",
            type,
            blockchains,
            continuation,
            size,
            sort,
            result.activities.size,
            result.continuation,
            result.cursor
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
        val filter = filterConverter.convertGetActivitiesByItem(type, itemId.fullId(), effectiveCursor)
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
        if (ff.enableEsActivitySource) {
            return esActivities.mapNotNull { it.activityDto }
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
        }.let { enrichmentActivityService.enrich(it) }

        val activitiesIdMapping = activities.associateBy { it.id.fullId() }
        return esActivities.mapNotNull { esActivity ->
            applyCursor(activitiesIdMapping[esActivity.activityId], esActivity.fromActivity().toString())
        }
    }

    private fun convertSort(sort: ActivitySortDto?): EsActivitySort {
        val latestFirst = when (sort) {
            ActivitySortDto.LATEST_FIRST, null -> true
            ActivitySortDto.EARLIEST_FIRST -> false
        }
        return EsActivitySort(latestFirst)
    }

    private fun applyCursor(activityDto: ActivityDto?, cursor: String): ActivityDto? {
        if (activityDto == null) return null
        return when (activityDto) {
            is MintActivityDto -> activityDto.copy(cursor = cursor)
            is BurnActivityDto -> activityDto.copy(cursor = cursor)
            is TransferActivityDto -> activityDto.copy(cursor = cursor)
            is OrderMatchSwapDto -> activityDto.copy(cursor = cursor)
            is OrderMatchSellDto -> activityDto.copy(cursor = cursor)
            is OrderBidActivityDto -> activityDto.copy(cursor = cursor)
            is OrderListActivityDto -> activityDto.copy(cursor = cursor)
            is OrderCancelBidActivityDto -> activityDto.copy(cursor = cursor)
            is OrderCancelListActivityDto -> activityDto.copy(cursor = cursor)
            is AuctionOpenActivityDto -> activityDto.copy(cursor = cursor)
            is AuctionBidActivityDto -> activityDto.copy(cursor = cursor)
            is AuctionFinishActivityDto -> activityDto.copy(cursor = cursor)
            is AuctionCancelActivityDto -> activityDto.copy(cursor = cursor)
            is AuctionStartActivityDto -> activityDto.copy(cursor = cursor)
            is AuctionEndActivityDto -> activityDto.copy(cursor = cursor)
            is L2DepositActivityDto -> activityDto.copy(cursor = cursor)
            is L2WithdrawalActivityDto -> activityDto.copy(cursor = cursor)
        }
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
}
