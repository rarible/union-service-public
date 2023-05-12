package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.client.ActivityType
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.client.TransferFilter
import com.rarible.protocol.union.integration.immutablex.converter.ImxActivityConverter
import kotlinx.coroutines.coroutineScope
import java.time.Instant

class ImxActivityService(
    private val client: ImxActivityClient,
    private val orderClient: ImxOrderClient,
    private val imxActivityConverter: ImxActivityConverter
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ActivityService {

    // TODO IMMUTABLEX move out to configuration
    private val byUserRequestChunkSize = 8

    // TODO originally, we can support BURNs here
    private val allowedTypes = mapOf(
        ActivityTypeDto.MINT to ActivityType.MINT,
        ActivityTypeDto.BURN to ActivityType.BURN,
        ActivityTypeDto.TRANSFER to ActivityType.TRANSFER,
        ActivityTypeDto.SELL to ActivityType.TRADE,
    )
    private val allowedUserTypes = mapOf(
        UserActivityTypeDto.MINT to ActivityType.MINT,
        UserActivityTypeDto.BURN to ActivityType.BURN,
        UserActivityTypeDto.TRANSFER_FROM to ActivityType.TRANSFER,
        //UserActivityTypeDto.SELL to ActivityType.TRADE, // TODO IMMUTABLEX filter by user is not supported
    )

    private val allowedItemAndOwnerTypes = mapOf(
        ItemAndOwnerActivityType.MINT to ActivityType.MINT,
        ItemAndOwnerActivityType.TRANSFER to ActivityType.TRANSFER
    )

    private fun mapTypes(types: List<ActivityTypeDto>) = types
        .ifEmpty { allowedTypes.keys }
        .mapNotNull { allowedTypes[it] }

    private fun mapUserTypes(types: List<UserActivityTypeDto>) = types
        .ifEmpty { allowedUserTypes.keys }
        .mapNotNull { allowedUserTypes[it] }

    private fun mapItemAndOwnerTypes(types: List<ItemAndOwnerActivityType>) = types
        .ifEmpty { allowedItemAndOwnerTypes.keys }
        .mapNotNull { allowedItemAndOwnerTypes[it] }

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<UnionActivity> {
        val result = getActivities(
            types = mapTypes(types),
            continuation = continuation,
            size = size,
            sort = sort
        )
        return convert(result)
    }

    override suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<UnionActivity> {
        val activitySort = when (sort) {
            SyncSortDto.DB_UPDATE_ASC -> ActivitySortDto.EARLIEST_FIRST
            SyncSortDto.DB_UPDATE_DESC -> ActivitySortDto.LATEST_FIRST
            else -> ActivitySortDto.EARLIEST_FIRST
        }
        val types = when (type) {
            SyncTypeDto.NFT -> listOf(ActivityType.TRANSFER, ActivityType.MINT)
            SyncTypeDto.ORDER -> listOf(ActivityType.TRADE)
            else -> ActivityType.values().toList()
        }
        val result = getActivities(
            types = types,
            continuation = continuation,
            size = size,
            sort = activitySort
        )
        return convert(result)
    }

    override suspend fun getAllRevertedActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<UnionActivity> {
        return Slice.empty()
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<UnionActivity> {
        val result = getActivities(
            types = mapTypes(types),
            token = collection,
            continuation = continuation,
            size = size,
            sort = sort
        )
        return convert(result)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<UnionActivity> {
        val (token, rawTokenId) = IdParser.split(itemId, 2)
        val tokenId = TokenIdDecoder.decode(rawTokenId)
        val result = getActivities(
            types = mapTypes(types),
            token = token,
            tokenId = tokenId,
            continuation = continuation,
            size = size,
            sort = sort
        )
        return convert(result)
    }

    override suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<UnionActivity> {
        val (token, rawTokenId) = IdParser.split(itemId, 2)
        val tokenId = TokenIdDecoder.decode(rawTokenId)
        val result = getActivities(
            types = mapItemAndOwnerTypes(types),
            token = token,
            tokenId = tokenId,
            user = owner,
            size = size,
            continuation = continuation,
            sort = sort,
        )
        return convert(result)
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<UnionActivity> {
        val result = coroutineScope {
            users.chunked(byUserRequestChunkSize).map { chunk ->
                chunk.mapAsync {
                    getActivities(
                        types = mapUserTypes(types),
                        user = it,
                        from = from,
                        to = to,
                        continuation = continuation,
                        size = size,
                        sort = sort,
                    )
                }.map { it.entities }.flatten()
            }.flatten()
        }

        return convert(toSlice(size, sort, result))
    }

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<UnionActivity> {
        val grouped = ids.filter {
            allowedTypes.containsKey(it.type)
        }.groupBy({ allowedTypes[it.type]!! }, { it.id })

        val result = grouped.mapAsync { group ->
            when (group.key) {
                ActivityType.MINT -> client.getMints(group.value)
                // Burns are originally transfers with zero 'receiver' address
                ActivityType.TRANSFER, ActivityType.BURN -> client.getTransfers(group.value)
                ActivityType.TRADE -> client.getTrades(group.value)
            }
        }.flatten()

        return convert(result)
    }

    private suspend fun getActivities(
        size: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto? = null,
        types: Collection<ActivityType>
    ): Slice<ImmutablexEvent> = coroutineScope {

        val safeSort = sort ?: ActivitySortDto.LATEST_FIRST
        val containsBurns = types.contains(ActivityType.BURN)
        val containsTransfers = types.contains(ActivityType.TRANSFER)

        val result = types.mapAsync {
            when (it) {
                ActivityType.MINT ->
                    client.getMints(size, continuation, token, tokenId, from, to, user, safeSort).result
                ActivityType.TRADE ->
                    client.getTrades(size, continuation, token, tokenId, from, to, user, safeSort).result
                ActivityType.TRANSFER -> {
                    if (containsBurns) {
                        // Both transfers and burns requested - we can retrieve them it in a single request
                        client.getTransfers(
                            size, continuation, token, tokenId, from, to, user, TransferFilter.ALL, safeSort
                        ).result
                    } else {
                        // If we need to get only transfers, we have to filter burns in memory
                        val transfers = ArrayList<ImmutablexTransfer>()
                        var transferContinuation = continuation
                        do {
                            val page = client.getTransfers(
                                size, transferContinuation, token, tokenId, from, to, user, TransferFilter.ALL, safeSort
                            )
                            val notBurns = page.result.filter { tr -> tr.receiver != ImmutablexTransfer.ZERO_ADDRESS }

                            transfers.addAll(notBurns)
                            transferContinuation = page.result.lastOrNull()?.let { last ->
                                DateIdContinuation(last.timestamp, last.transactionId.toString()).toString()
                            }
                            // Stop if page is full or returned page is the last page
                        } while (transfers.size < size && page.result.size == size)
                        transfers.take(size)
                    }
                }
                ActivityType.BURN -> {
                    // Will be retrieved in TRANSFER branch, together with transfers
                    if (containsTransfers) {
                        emptyList()
                    } else {
                        client.getTransfers(
                            size, continuation, token, tokenId, from, to, user, TransferFilter.BURNS, safeSort
                        ).result
                    }
                }
            }
        }.flatten()

        toSlice(size, sort, result)
    }

    // Since we need to retrieve additional data for some types of activities, we need to operate
    // raw data here and enrich it only when it already trimmed in order to avoid unnecessary API calls
    private fun toSlice(
        size: Int,
        sort: ActivitySortDto?,
        activities: List<ImmutablexEvent>
    ): Slice<ImmutablexEvent> {

        val continuationFactory = when (sort ?: ActivitySortDto.LATEST_FIRST) {
            ActivitySortDto.LATEST_FIRST -> ByLastUpdatedAndIdDesc
            ActivitySortDto.EARLIEST_FIRST -> ByLastUpdatedAndIdAsc
        }

        return Paging(continuationFactory, activities).getSlice(size)
    }

    // Here we have the slice with requested size and only here we can execute additional call to
    // fulfill additional data
    private suspend fun convert(slice: Slice<ImmutablexEvent>): Slice<UnionActivity> {
        return Slice(
            // Here continuation implemented in the same way as for DTO, we can just pass it
            continuation = slice.continuation,
            entities = convert(slice.entities)
        )
    }

    suspend fun getTradeOrders(activities: List<ImmutablexEvent>): Map<Long, ImmutablexOrder> {
        val orderIds = activities.flatMap {
            when (it) {
                // For 'Trade' we should get orders to fulfill sides
                is ImmutablexTrade -> listOf(it.make.orderId.toString(), it.take.orderId.toString())
                else -> emptyList()
            }
        }
        return if (orderIds.isEmpty()) {
            emptyMap()
        } else {
            orderClient.getByIds(orderIds).associateBy { it.orderId }
        }
    }

    suspend fun convert(activities: List<ImmutablexEvent>): List<UnionActivity> {
        val orders = getTradeOrders(activities)
        return activities.mapAsync {
            imxActivityConverter.convert(it, orders, blockchain)
        }
    }

    object ByLastUpdatedAndIdDesc : ContinuationFactory<ImmutablexEvent, DateIdContinuation> {

        override fun getContinuation(entity: ImmutablexEvent): DateIdContinuation {
            return DateIdContinuation(entity.timestamp, entity.activityId.value, false)
        }
    }

    object ByLastUpdatedAndIdAsc : ContinuationFactory<ImmutablexEvent, DateIdContinuation> {

        override fun getContinuation(entity: ImmutablexEvent): DateIdContinuation {
            return DateIdContinuation(entity.timestamp, entity.activityId.value, true)
        }
    }
}
