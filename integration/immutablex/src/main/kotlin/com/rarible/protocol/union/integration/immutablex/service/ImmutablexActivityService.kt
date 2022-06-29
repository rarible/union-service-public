package com.rarible.protocol.union.integration.immutablex.service

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
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexActivityConverter
import java.time.Instant

class ImmutablexActivityService(
    private val client: ImmutablexApiClient,
    private val converter: ImmutablexActivityConverter,
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ActivityService {

    private val allowedTypes = setOf(
        ActivityTypeDto.MINT,
        ActivityTypeDto.TRANSFER,
        ActivityTypeDto.SELL,
    )
    private val allowedUserTypes = setOf(
        UserActivityTypeDto.MINT,
        UserActivityTypeDto.TRANSFER_FROM,
        UserActivityTypeDto.SELL,
    )

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val resultTypes = if (types.isEmpty()) allowedTypes else allowedTypes.intersect(types.toSet())
        val result = resultTypes
            .flatMap { type ->
                when (type) {
                    ActivityTypeDto.MINT ->
                        client.getMints(size, continuation, sort = sort).result
                    ActivityTypeDto.TRANSFER ->
                        client.getTransfers(size, continuation, sort = sort).result
                    ActivityTypeDto.SELL ->
                        client.getTrades(size, continuation, sort = sort).result
                    else -> emptyList()
                }
            }.asSequence()
            .map { converter.convert(it) }
            .sortedBy { it.date }
            .take(size)
            .let {
                if (sort == ActivitySortDto.LATEST_FIRST) it.toList().asReversed() else it.toList()
            }
        return Paging(ActivityContinuation.ByLastUpdatedAndIdDesc, result).getSlice(size)
    }

    override suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> = Slice.empty()

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> = Slice.empty()

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val resultTypes = if (types.isEmpty()) allowedTypes else allowedTypes.intersect(types.toSet())
        val result = resultTypes
            .flatMap { type ->
                when (type) {
                    ActivityTypeDto.MINT ->
                        client.getMints(size, continuation, itemId, sort = sort).result
                    ActivityTypeDto.TRANSFER ->
                        client.getTransfers(size, continuation, itemId, sort = sort).result
                    ActivityTypeDto.SELL ->
                        client.getTrades(size, continuation, itemId, sort = sort).result
                    else -> emptyList()
                }
            }.asSequence()
            .map { converter.convert(it) }
            .sortedBy { it.date }
            .take(size)
            .let {
                if (sort == ActivitySortDto.LATEST_FIRST) it.toList().asReversed() else it.toList()
            }
        return Paging(ActivityContinuation.ByLastUpdatedAndIdDesc, result).getSlice(size)
    }

    override suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val result = types.flatMap { type ->
            when (type) {
                ItemAndOwnerActivityType.MINT ->
                    client.getMints(size, continuation, itemId, user = owner, sort = sort).result
                ItemAndOwnerActivityType.TRANSFER ->
                    client.getTransfers(size, continuation, itemId, user = owner, sort = sort).result
                // todo: handle transfers with purchased = true
                // client.getTrades(size, continuation, itemId, user = owner, sort = sort).result
                else -> emptyList()
            }
        }.asSequence()
            .map { converter.convert(it) }
            .sortedBy { it.date }
            .take(size)
            .let {
                if (sort == ActivitySortDto.LATEST_FIRST) it.toList().asReversed() else it.toList()
            }
        return Paging(ActivityContinuation.ByLastUpdatedAndIdDesc, result).getSlice(size)
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
        val resultTypes = if (types.isEmpty()) allowedUserTypes else allowedUserTypes.intersect(types.toSet())
        val result = resultTypes
            .flatMap { type ->
                users.flatMap { user ->
                    when (type) {
                        UserActivityTypeDto.MINT ->
                            client.getMints(size, continuation, null, from, to, user, sort = sort).result
                        UserActivityTypeDto.TRANSFER_FROM ->
                            client.getTransfers(size, continuation, null, from, to, user, sort = sort).result
                        UserActivityTypeDto.SELL ->
                            client.getTrades(size, continuation, null, from, to, user, sort = sort).result
                        else -> emptyList()
                    }
                }
            }.asSequence()
            .map { converter.convert(it) }
            .sortedBy { it.date }
            .take(size)
            .let {
                if (sort == ActivitySortDto.LATEST_FIRST) it.toList().asReversed() else it.toList()
            }
        return Paging(ActivityContinuation.ByLastUpdatedAndIdDesc, result).getSlice(size)
    }

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<ActivityDto> {
        TODO("To be implemented under ALPHA-276")
    }
}
