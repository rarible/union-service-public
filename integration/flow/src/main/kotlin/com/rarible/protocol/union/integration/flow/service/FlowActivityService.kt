package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesByIdRequestDto
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
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
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

@CaptureSpan(type = "blockchain")
open class FlowActivityService(
    private val activityControllerApi: FlowNftOrderActivityControllerApi,
    private val flowActivityConverter: FlowActivityConverter
) : AbstractBlockchainService(BlockchainDto.FLOW), ActivityService {

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val activities = activityControllerApi.getNftOrderAllActivities(
            types.map { it.name },
            continuation,
            size,
            sort?.name
        ).awaitFirst()
        return result(activities, size, sort)
    }

    override suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> {
        if (type == SyncTypeDto.AUCTION) {
            return Slice.empty()
        }

        val flowSort = flowActivityConverter.convert(sort)
        val flowTypes = flowActivityConverter.convert(type)

        val result = activityControllerApi.getNftOrderActivitiesSync(
            flowTypes,
            continuation,
            size,
            flowSort
        ).awaitFirst()

        return flowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val activities = activityControllerApi.getNftOrderActivitiesByCollection(
            types.map { it.name },
            collection,
            continuation,
            size,
            sort?.name
        ).awaitFirst()

        return result(activities, size, sort)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val activities = activityControllerApi.getNftOrderActivitiesByItem(
            types.map { it.name },
            contract,
            tokenId.toLong(),
            continuation,
            size,
            sort?.name
        ).awaitFirst()
        return result(activities, size, sort)
    }

    override suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        return Slice.empty() // TODO Not implemented
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val activities = activityControllerApi.getNftOrderActivitiesByUser(
            types.map { it.name },
            users,
            from?.toEpochMilli(),
            to?.toEpochMilli(),
            continuation,
            size,
            sort?.name
        ).awaitFirst()
        return result(activities, size, sort)
    }

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<ActivityDto> {
        val result = activityControllerApi.getNftOrderActivitiesById(NftActivitiesByIdRequestDto(ids.map { it.id }))
            .awaitFirst()
        return flowActivityConverter.convert(result)
    }

    private fun ActivitySortDto?.toFactory() = when(this) {
        ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
        ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
    }

    private suspend fun result(activities: FlowActivitiesDto, size: Int, sort: ActivitySortDto?): Slice<ActivityDto> {
        return Paging(
            sort.toFactory(),
            flowActivityConverter.convert(activities)
        ).getSlice(size)
    }
}
