package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesByIdRequestDto
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.union.core.continuation.UnionActivityContinuation
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
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
    ): Slice<UnionActivity> {
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
    ): Slice<UnionActivity> {
        if (type == SyncTypeDto.AUCTION) {
            return Slice.empty()
        }

        val flowSort = flowActivityConverter.convert(sort)
        val flowTypes = flowActivityConverter.convert(type)

        val activities = activityControllerApi.getNftOrderActivitiesSync(
            flowTypes,
            continuation,
            size,
            flowSort
        ).awaitFirst()

        return result(activities,size, sort)
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
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
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
    ): Slice<UnionActivity> {
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
    ): Slice<UnionActivity> {
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
    ): Slice<UnionActivity> {
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

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<UnionActivity> {
        val result = activityControllerApi.getNftOrderActivitiesById(NftActivitiesByIdRequestDto(ids.map { it.id }))
            .awaitFirst()
        return flowActivityConverter.convert(result)
    }

    private fun ActivitySortDto?.toFactory() = when (this) {
        ActivitySortDto.LATEST_FIRST, null -> UnionActivityContinuation.ByLastUpdatedAndIdDesc
        ActivitySortDto.EARLIEST_FIRST -> UnionActivityContinuation.ByLastUpdatedAndIdAsc
    }

    private fun SyncSortDto?.toFactory() = when (this) {
        SyncSortDto.DB_UPDATE_ASC, null -> UnionActivityContinuation.ByLastUpdatedSyncAndIdAsc
        SyncSortDto.DB_UPDATE_DESC -> UnionActivityContinuation.ByLastUpdatedSyncAndIdDesc
    }

    private suspend fun result(
        activities: FlowActivitiesDto,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        return resultAll(activities, size, sort.toFactory())
    }

    private suspend fun result(activities: FlowActivitiesDto, size: Int, sort: SyncSortDto?): Slice<UnionActivity> {
        return resultAll(activities, size, sort.toFactory())
    }

    private suspend fun resultAll(
        activities: FlowActivitiesDto,
        size: Int,
        sortFactory: ContinuationFactory<UnionActivity, DateIdContinuation>
    ): Slice<UnionActivity> {
        return Paging(
            sortFactory,
            flowActivityConverter.convert(activities)
        ).getSlice(size)
    }
}
