package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto

class DummyOwnershipService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), OwnershipService {

    override suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        return Page.empty()
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        throw UnionNotFoundException("Ownership [$ownershipId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        return Page.empty()
    }
}