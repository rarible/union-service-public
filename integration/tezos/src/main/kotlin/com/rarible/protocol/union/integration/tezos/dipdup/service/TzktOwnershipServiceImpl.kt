package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktOwnershipConverter
import com.rarible.tzkt.client.OwnershipClient

class TzktOwnershipServiceImpl(val ownershipClient: OwnershipClient): TzktOwnershipService {

    override fun enabled() = true

    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val tzktOwnership = ownershipClient.ownershipById(ownershipId)
        return TzktOwnershipConverter.convert(tzktOwnership, blockchain)
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val tzktPage = ownershipClient.ownershipsByToken(itemId, size, continuation)
        return TzktOwnershipConverter.convert(tzktPage, blockchain)
    }

}
