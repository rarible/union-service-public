package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowActivityService(
    blockchain: BlockchainDto,
    private val activityControllerApi: FlowNftOrderActivityControllerApi
) : AbstractFlowService(blockchain), ActivityService {

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderAllActivities(rawTypes, continuation, size)
            .awaitFirst()
        return FlowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByCollection(rawTypes, collection, continuation, size)
            .awaitFirst()
        return FlowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByItem(
            rawTypes,
            contract,
            tokenId.toLong(),
            continuation,
            size
        ).awaitFirst()
        return FlowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByUser(rawTypes, users, continuation, size, sort?.name)
            .awaitFirst()
        return FlowActivityConverter.convert(result, blockchain)
    }
}
