package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.continuation.Page
import com.rarible.protocol.union.dto.UnionOwnershipDto

interface OwnershipService : BlockchainService {

    suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): Page<UnionOwnershipDto>

    suspend fun getOwnershipById(
        ownershipId: String
    ): UnionOwnershipDto

    suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnershipDto>

}