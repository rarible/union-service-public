package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto

interface OwnershipService : BlockchainService {

    suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): UnionOwnershipsDto

    suspend fun getOwnershipById(
        ownershipId: String
    ): UnionOwnershipDto

    suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): UnionOwnershipsDto

}