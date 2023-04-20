package com.rarible.protocol.union.enrichment.service.query.activity

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.continuation.UnionActivityContinuation
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.util.BlockchainFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityApiMergeService(
    private val router: BlockchainRouter<ActivityService>,
    private val enrichmentActivityService: EnrichmentActivityService
) : ActivityQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (result, slicesCounter) = if (null == cursor) {
            val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)
            val blockchainMap = coroutineScope {
                evaluatedBlockchains.associateWith {
                    async {
                        router.getService(it).getAllActivities(type, continuation, safeSize, sort)
                    }
                }.mapValues { it.value.await() }
            }
            val dto = merge(blockchainMap, safeSize, sort)
            Pair(dto, blockchainMap.map { it.value.entities.size })
        } else {
            val slices = getAllActivities(type, blockchains, cursor, safeSize, sort)
            val dto = toDtoWithCursor(ArgPaging(continuationFactory(sort), slices).getSlice(safeSize))
            Pair(dto, slices.map { it.slice.entities.size })
        }
        logger.info(
            "Response for getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={}) from blockchain slices {} ",
            type,
            blockchains,
            continuation,
            size,
            sort,
            result.activities.size,
            result.continuation,
            result.cursor,
            slicesCounter
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
        val safeSize = PageSize.ACTIVITY.limit(size)
        val activitySlice = router.getService(blockchain).getAllActivitiesSync(continuation, safeSize, sort, type)
        val dto = toDto(activitySlice)
        logger.info(
            "Response for getAllActivitiesSync(type={}, continuation={}, size={}, sort={}): " +
                "Slice(size={}, continuation={}) ",
            type, continuation, size, sort, dto.activities.size, dto.continuation
        )
        return dto
    }

    override suspend fun getAllRevertedActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val activitySlice = router.getService(blockchain)
            .getAllRevertedActivitiesSync(continuation, safeSize, sort, type)
        val dto = toDto(activitySlice)
        logger.info(
            "Response for getRevertedActivitiesSync(continuation={}, size={}, ty[e={}, sort={}): " +
                "Slice(size={}, continuation={}) ",
            continuation, size, type, sort, dto.activities.size, dto.continuation
        )
        return dto
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<CollectionIdDto>,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val collectionId = collection.first() // fallback for API variant
        val blockchain = collectionId.blockchain
        val dto = withCursor(continuation, cursor, blockchain, size, sort) { cont, safeSize ->
            router.getService(collectionId.blockchain)
                .getActivitiesByCollection(type, collectionId.value, cont, safeSize, sort)
        }
        logger.info(
            "Response for getActivitiesByCollection(type={}, collection={}, continuation={}, size={}, sort={}): " +
                "Slice(size={}, continuation={}) ",
            type, collectionId.fullId(), continuation, size, sort, dto.activities.size, dto.continuation
        )
        return dto
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: ItemIdDto,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val dto = withCursor(continuation, cursor, itemId.blockchain, size, sort) { cont, safeSize ->
            router.getService(itemId.blockchain)
                .getActivitiesByItem(
                    type,
                    itemId.value,
                    cont,
                    safeSize,
                    sort
                )
        }
        logger.info(
            "Response for getActivitiesByItem(type={}, itemId={} continuation={}, size={}, sort={}): " +
                "Slice(size={}, continuation={}) ",
            type, itemId.fullId(), continuation, size, sort, dto.activities.size, dto.continuation
        )
        return dto
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
        val safeSize = PageSize.ACTIVITY.limit(size)
        val filter = BlockchainFilter(blockchains)
        val groupedByBlockchain = user
            // Since user specified here with blockchain group, we need to route request to all subchains
            .flatMap { address -> filter.exclude(address.blockchainGroup).map { it to address.value } }
            .groupBy({ it.first }, { it.second })

        val (result, slicesCounter) = if (null == cursor) {
            val blockchainMap = coroutineScope {
                groupedByBlockchain.mapValues {
                    val blockchain = it.key
                    val blockchainUsers = it.value
                    async {
                        router.getService(blockchain)
                            .getActivitiesByUser(type, blockchainUsers, from, to, continuation, safeSize, sort)
                    }
                }.mapValues { it.value.await() }
            }
            val result = merge(blockchainMap, safeSize, sort)
            Pair(result, blockchainMap.map { it.value.entities.size })
        } else {
            val slices = getActivitiesByUser(type, groupedByBlockchain, from, to, cursor, safeSize, sort)
            val dto = toDtoWithCursor(ArgPaging(continuationFactory(sort), slices).getSlice(safeSize))
            Pair(dto, slices.map { it.slice.entities.size })
        }
        logger.info(
            "Response for getActivitiesByUser(type={}, users={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={}) from user slices {} ",
            type, user, continuation, size, sort,
            result.activities.size, result.continuation, result.cursor, slicesCounter
        )
        return result
    }

    private suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        size: Int,
        sort: ActivitySortDto?
    ): List<ArgSlice<UnionActivity>> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices = getActivitiesByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getAllActivities(type, continuation, size, sort)
        }
        return slices
    }

    private suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        groupedByBlockchain: Map<BlockchainDto, List<String>>,
        from: Instant?,
        to: Instant?,
        cursor: String?,
        safeSize: Int,
        sort: ActivitySortDto?
    ): List<ArgSlice<UnionActivity>> {
        val evaluatedBlockchains = groupedByBlockchain.keys.map { it.name }
        val slices = getActivitiesByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            val users = groupedByBlockchain[blockDto] ?: listOf()
            router.getService(blockDto).getActivitiesByUser(type, users, from, to, continuation, safeSize, sort)
        }
        return slices
    }

    private suspend fun merge(
        blockchainPages: Map<BlockchainDto, Slice<UnionActivity>>,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val factory = continuationFactory(sort)
        val slices = blockchainPages.map { ArgSlice(it.key.name, it.value.continuation, it.value) }
        val finalSlice = ArgPaging(factory, slices).getSlice(size)
        val dto = toDtoWithCursor(finalSlice)
        val continuation = if (finalSlice.entities.size >= size) finalSlice.entities.last()
            .let { factory.getContinuation(it).toString() } else null
        return dto.copy(continuation = continuation)
    }

    private suspend fun toDtoWithCursor(slice: Slice<UnionActivity>): ActivitiesDto {
        return ActivitiesDto(
            continuation = null,
            cursor = slice.continuation,
            activities = enrichmentActivityService.enrich(slice.entities)
        )
    }

    private suspend fun toDto(slice: Slice<UnionActivity>): ActivitiesDto {
        return ActivitiesDto(
            continuation = slice.continuation,
            cursor = slice.continuation,
            activities = enrichmentActivityService.enrich(slice.entities)
        )
    }

    private suspend fun withCursor(
        continuation: String?,
        cursor: String?,
        blockchain: BlockchainDto,
        size: Int?,
        sort: ActivitySortDto?,
        clientCall: suspend (continuation: String?, safeSize: Int) -> Slice<UnionActivity>
    ): ActivitiesDto {
        val factory = continuationFactory(sort)
        val safeSize = PageSize.ACTIVITY.limit(size)
        val blockchainContinuation = if (cursor == null) {
            continuation
        } else {
            val combinedContinuation = CombinedContinuation.parse(cursor)
            combinedContinuation.continuations[blockchain.name]
        }
        val slice = if (blockchainContinuation == ArgSlice.COMPLETED) {
            Slice(null, emptyList())
        } else {
            clientCall(blockchainContinuation, safeSize)
        }
        val cont = if (slice.entities.size >= safeSize) slice.entities.last()
            .let { factory.getContinuation(it).toString() } else null
        val argv = ArgSlice(blockchain.name, blockchainContinuation, slice)
        val finalSlice = ArgPaging(factory, listOf(argv)).getSlice(safeSize)
        return ActivitiesDto(
            continuation = cont,
            cursor = finalSlice.continuation,
            activities = enrichmentActivityService.enrich(finalSlice.entities)
        )
    }

    private fun continuationFactory(sort: ActivitySortDto?) = when (sort) {
        ActivitySortDto.EARLIEST_FIRST -> UnionActivityContinuation.ByLastUpdatedAsc
        ActivitySortDto.LATEST_FIRST, null -> UnionActivityContinuation.ByLastUpdatedDesc
    }

    private suspend fun getActivitiesByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Slice<UnionActivity>
    ): List<ArgSlice<UnionActivity>> {
        val currentContinuation = CombinedContinuation.parse(continuation)

        return blockchains.mapAsync { blockchain ->
            val blockchainContinuation = currentContinuation.continuations[blockchain]
            // For completed blockchain we do not request orders
            if (blockchainContinuation == ArgSlice.COMPLETED) {
                ArgSlice(blockchain, blockchainContinuation, Slice(null, emptyList()))
            } else {
                ArgSlice(blockchain, blockchainContinuation, clientCall(blockchain, blockchainContinuation))
            }
        }
    }
}
