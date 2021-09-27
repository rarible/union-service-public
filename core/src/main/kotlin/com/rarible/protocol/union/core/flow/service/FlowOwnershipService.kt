package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowUnionOwnershipConverter
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowOwnershipService(
    blockchain: BlockchainDto,
    private val ownershipControllerApi: FlowNftOwnershipControllerApi
) : AbstractFlowService(blockchain), OwnershipService {

    override suspend fun getAllOwnerships(continuation: String?, size: Int): UnionOwnershipsDto {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(continuation, size).awaitFirst()
        return FlowUnionOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnershipDto {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return FlowUnionOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): UnionOwnershipsDto {
        val items = ownershipControllerApi.getNftOwnershipsByItem(contract, tokenId, continuation, size).awaitFirst()
        return FlowUnionOwnershipConverter.convert(items, blockchain)
    }
}