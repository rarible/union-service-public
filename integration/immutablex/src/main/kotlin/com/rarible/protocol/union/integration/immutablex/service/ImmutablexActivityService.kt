package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivityDto
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
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexActivityConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import kotlinx.coroutines.coroutineScope
import java.time.Instant

class ImmutablexActivityService(
    private val client: ImmutablexActivityClient,
    private val orderService: ImmutablexOrderService
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ActivityService {

    // TODO IMMUTABLEX move out to configuration
    private val byUserRequestChunkSize = 8

    private val allowedTypes = mapOf(
        ActivityTypeDto.MINT to ActivityType.MINT,
        ActivityTypeDto.TRANSFER to ActivityType.TRANSFER,
        ActivityTypeDto.SELL to ActivityType.TRADE,
    )
    private val allowedUserTypes = mapOf(
        UserActivityTypeDto.MINT to ActivityType.MINT,
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
    ): Slice<ActivityDto> {
        val result = getActivities(
            types = mapTypes(types),
            continuation = continuation,
            size = size,
            sort = sort
        )
        return toSliceDto(result)
    }

    override suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> = Slice.empty() // TODO IMMUTABLEX is there way to implement it

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val result = getActivities(
            types = mapTypes(types),
            token = collection,
            continuation = continuation,
            size = size,
            sort = sort
        )
        return toSliceDto(result)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val (token, tokenId) = IdParser.split(itemId, 2)
        val result = getActivities(
            types = mapTypes(types),
            token = token,
            tokenId = tokenId,
            continuation = continuation,
            size = size,
            sort = sort
        )
        return toSliceDto(result)
    }

    override suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val (token, tokenId) = IdParser.split(itemId, 2)
        val result = getActivities(
            types = mapItemAndOwnerTypes(types),
            token = token,
            tokenId = tokenId,
            user = owner,
            size = size,
            continuation = continuation,
            sort = sort,
        )
        return toSliceDto(result)
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
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

        return toSliceDto(toSlice(size, sort, result))
    }

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<ActivityDto> {
        // TODO IMMUTABLEX is there way to implement it?
        return emptyList()
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

        val result = types.mapAsync {
            when (it) {
                ActivityType.MINT ->
                    client.getMints(size, continuation, token, tokenId, from, to, user, safeSort).result
                ActivityType.TRANSFER ->
                    client.getTransfers(size, continuation, token, tokenId, from, to, user, safeSort).result
                ActivityType.TRADE ->
                    client.getTrades(size, continuation, token, tokenId, from, to, user, safeSort).result
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
    private suspend fun toSliceDto(slice: Slice<ImmutablexEvent>): Slice<ActivityDto> {
        val orderIds = slice.entities.flatMap {
            when (it) {
                // For 'Trade' we should get orders to fulfill sides
                is ImmutablexTrade -> listOf(it.make.orderId.toString(), it.take.orderId.toString())
                else -> emptyList()
            }
        }
        val orders = if (orderIds.isEmpty()) {
            emptyMap()
        } else {
            orderService.getOrdersByIds(orderIds).associateBy { it.id.value.toLong() }
        }

        return Slice(
            continuation = slice.continuation,
            entities = slice.entities.map { ImmutablexActivityConverter.convert(it, orders) }
        )
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
