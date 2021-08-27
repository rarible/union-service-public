package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionActivitiesDto
import org.springframework.http.ResponseEntity

class ActivityController : ActivityControllerApi {

    override suspend fun getAllActivities(
        type: List<String>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionActivitiesDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionActivitiesDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionActivitiesDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getActivitiesByUser(
        type: List<String>,
        user: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionActivitiesDto> {
        TODO("Not yet implemented")
    }
}