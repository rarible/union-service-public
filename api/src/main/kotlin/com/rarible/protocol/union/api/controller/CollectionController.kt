package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.continuation.ContinuationPaging
import com.rarible.protocol.union.core.service.CollectionServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto
import com.rarible.protocol.union.dto.continuation.UnionCollectionContinuation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController(
    private val router: CollectionServiceRouter
) : CollectionControllerApi {

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionCollectionsDto> {
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllCollections(continuation, size)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = ContinuationPaging(
            UnionCollectionContinuation.ById,
            blockchainPages.flatMap { it.collections }
        ).getPage(size)

        val result = UnionCollectionsDto(total, combinedPage.printContinuation(), combinedPage.entities)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<UnionCollectionDto> {
        val (blockchain, shortCollectionId) = IdParser.parse(collection)
        val result = router.getService(blockchain).getCollectionById(shortCollectionId)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionCollectionsDto> {
        val (blockchain, shortOwner) = IdParser.parse(owner)
        val result = router.getService(blockchain).getCollectionsByOwner(shortOwner, continuation, size)
        return ResponseEntity.ok(result)
    }
}