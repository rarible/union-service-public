package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.continuation.page.Page

interface OwnershipService : BlockchainService {

    suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): Page<UnionOwnership>

    suspend fun getOwnershipById(
        ownershipId: String
    ): UnionOwnership

    suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership>

}