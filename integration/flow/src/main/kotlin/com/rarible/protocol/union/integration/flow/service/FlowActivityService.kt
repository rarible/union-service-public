package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

@CaptureSpan(type = "ext", subtype = "blockchain")
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
        val result = activityControllerApi.getNftOrderAllActivities(
            types.map { it.name },
            continuation,
            size,
            sort?.name
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
        val result = activityControllerApi.getNftOrderActivitiesByCollection(
            types.map { it.name },
            collection,
            continuation,
            size,
            sort?.name
        ).awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val result = activityControllerApi.getNftOrderActivitiesByItem(
            types.map { it.name },
            contract,
            UnionConverter.convertToLong(tokenId),
            continuation,
            size,
            sort?.name
        ).awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
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
        val result = activityControllerApi.getNftOrderActivitiesByUser(
            types.map { it.name },
            users,
            from?.toEpochMilli(),
            to?.toEpochMilli(),
            continuation,
            size,
            sort?.name
        ).awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
    }
}
