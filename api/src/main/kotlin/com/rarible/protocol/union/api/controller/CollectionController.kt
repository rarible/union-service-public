package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController : CollectionControllerApi {

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionCollectionsDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<UnionCollectionDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionCollectionsDto> {
        TODO("Not yet implemented")
    }
}