package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class FlowOwnershipService(
    private val ownershipControllerApi: FlowNftOwnershipControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return FlowOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getAllOwnerships(ownershipIds: List<String>): List<UnionOwnership> {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val ownerships = ownershipControllerApi.getNftOwnershipsByItem(
            contract,
            tokenId.toString(),
            continuation,
            size
        ).awaitFirst()
        return FlowOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        return Page.empty()
    }
}
