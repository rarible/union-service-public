package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowOwnershipService(
    blockchain: BlockchainDto,
    private val ownershipControllerApi: FlowNftOwnershipControllerApi
) : AbstractFlowService(blockchain), OwnershipService {

    override suspend fun getAllOwnerships(continuation: String?, size: Int): OwnershipsDto {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(continuation, size).awaitFirst()
        return FlowOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipById(ownershipId: String): OwnershipDto {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return FlowOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): OwnershipsDto {
        val items = ownershipControllerApi.getNftOwnershipsByItem(contract, tokenId, continuation, size).awaitFirst()
        return FlowOwnershipConverter.convert(items, blockchain)
    }
}