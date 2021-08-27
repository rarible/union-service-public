package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto
import org.springframework.http.ResponseEntity

class OwnershipController : OwnershipControllerApi {

    override suspend fun getAllOwnerships(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOwnershipsDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnershipById(
        ownershipId: String
    ): ResponseEntity<UnionOwnershipDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOwnershipsDto> {
        TODO("Not yet implemented")
    }
}