package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst

class FlowOwnershipService(
    private val ownershipControllerApi: FlowNftOwnershipControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), OwnershipService {

    override suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(
            continuation,
            size
        ).awaitFirst()
        return FlowOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return FlowOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val items = ownershipControllerApi.getNftOwnershipsByItem(
            contract,
            UnionConverter.convertToLong(tokenId).toString(),
            continuation,
            size
        ).awaitFirst()
        return FlowOwnershipConverter.convert(items, blockchain)
    }
}