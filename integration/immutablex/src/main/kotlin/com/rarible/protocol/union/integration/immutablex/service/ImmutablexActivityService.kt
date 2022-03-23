package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.*
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
        val result = allowedTypes.intersect(types.toSet())
            .flatMap { type ->
                when (type) {
                    ActivityTypeDto.MINT ->
                        client.getMints(size, continuation).result
                    ActivityTypeDto.TRANSFER ->
                        client.getTransfers(size, continuation).result
                    ActivityTypeDto.SELL ->
                        client.getTrades(size, continuation).result
                    else -> emptyList()
                }
            }.asSequence()
            .map { converter.convert(it) }
            .sortedBy { it.date }
            .take(size)
            .let {
                if (sort == ActivitySortDto.LATEST_FIRST) it.toList().asReversed() else it.toList()
            }
        val c = result.lastOrNull()?.let {
            "${it.date.toEpochMilli()}_${it.id.fullId()}"
        }
        return Slice(c, result)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> = Slice("", emptyList())

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val result = allowedTypes.intersect(types.toSet())
            .flatMap { type ->
                when (type) {
                    ActivityTypeDto.MINT ->
                        client.getMints(size, continuation, itemId).result
                    ActivityTypeDto.TRANSFER ->
                        client.getTransfers(size, continuation, itemId).result
                    ActivityTypeDto.SELL ->
                        client.getTrades(size, continuation, itemId).result
                    else -> emptyList()
                }
            }.asSequence()
            .map { converter.convert(it) }
            .sortedBy { it.date }
            .take(size)
            .let {
                if (sort == ActivitySortDto.LATEST_FIRST) it.toList().asReversed() else it.toList()
            }
        val c = result.lastOrNull()?.let {
            "${it.date.toEpochMilli()}_${it.id.fullId()}"
        }
        return Slice(c, result)
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
        val result = allowedUserTypes.intersect(types.toSet())
            .flatMap { type ->
                users.flatMap { user ->
                    when (type) {
                        UserActivityTypeDto.MINT ->
                            client.getMints(size, continuation, null, from, to, user).result
                        UserActivityTypeDto.TRANSFER_FROM ->
                            client.getTransfers(size, continuation, null, from, to, user).result
                        UserActivityTypeDto.SELL ->
                            client.getTrades(size, continuation, null, from, to, user).result
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
        val c = result.lastOrNull()?.let {
            "${it.date.toEpochMilli()}_${it.id.fullId()}"
        }
        return Slice(c, result)
    }
}
