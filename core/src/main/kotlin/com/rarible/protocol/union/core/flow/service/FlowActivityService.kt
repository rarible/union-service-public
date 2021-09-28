package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowUnionActivityConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.dto.*
import kotlinx.coroutines.reactive.awaitFirst

class FlowActivityService(
    blockchain: BlockchainDto,
    private val activityControllerApi: FlowNftOrderActivityControllerApi
) : AbstractFlowService(blockchain), ActivityService {

    override suspend fun getAllActivities(
        types: List<UnionActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderAllActivities(rawTypes, continuation, size)
            .awaitFirst()
        return FlowUnionActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByCollection(
        types: List<UnionActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByCollection(rawTypes, collection, continuation, size)
            .awaitFirst()
        return FlowUnionActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByItem(
        types: List<UnionActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByItem(
            rawTypes,
            contract,
            tokenId.toLong(),
            continuation,
            size
        ).awaitFirst()
        return FlowUnionActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByUser(
        types: List<UnionUserActivityTypeDto>,
        users: List<String>,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByUser(rawTypes, users, continuation, size, sort?.name)
            .awaitFirst()
        return FlowUnionActivityConverter.convert(result, blockchain)
    }
}
