package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.continuation.CollectionContinuation
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.parser.IdParser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController(
    private val router: BlockchainRouter<CollectionService>
) : CollectionControllerApi {

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val safeSize = PageSize.COLLECTION.limit(size)
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllCollections(continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            CollectionContinuation.ById,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        return ResponseEntity.ok(toDto(combinedPage))
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<CollectionDto> {
        val collectionAddress = IdParser.parseAddress(collection)
        val result = router.getService(collectionAddress.blockchain).getCollectionById(collectionAddress.value)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val safeSize = PageSize.COLLECTION.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val result = router.getService(ownerAddress.blockchain)
            .getCollectionsByOwner(ownerAddress.value, continuation, safeSize)
        return ResponseEntity.ok(toDto(result))
    }

    private fun toDto(page: Page<CollectionDto>): CollectionsDto {
        return CollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = page.entities
        )
    }

}