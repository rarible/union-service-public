package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto

interface OwnershipService : BlockchainService {

    suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): OwnershipsDto

    suspend fun getOwnershipById(
        ownershipId: String
    ): OwnershipDto

    suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): OwnershipsDto

}