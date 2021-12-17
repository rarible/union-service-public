package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.CollectionContinuation
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController(
    private val router: BlockchainRouter<CollectionService>
) : CollectionControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

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

        logger.info("Response for getAllCollections(blockchains={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, combinedPage.entities.size, combinedPage.total,
            combinedPage.continuation, blockchainPages.map { it.entities.size }
        )
        return ResponseEntity.ok(toDto(combinedPage))
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<CollectionDto> {
        val collectionId = IdParser.parseContract(collection)
        val result = router.getService(collectionId.blockchain).getCollectionById(collectionId.value)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val safeSize = PageSize.COLLECTION.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val blockchainPages = router.executeForAll(ownerAddress.blockchainGroup.subchains()) {
            it.getCollectionsByOwner(ownerAddress.value, continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            CollectionContinuation.ById,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getCollectionsByOwner(owner={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={})",
            owner, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation
        )
        return ResponseEntity.ok(toDto(combinedPage))
    }

    private fun toDto(page: Page<CollectionDto>): CollectionsDto {
        return CollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = page.entities
        )
    }

}