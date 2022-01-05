package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.util.BlockchainFilter
import com.rarible.protocol.union.api.service.CollectionApiService
import com.rarible.protocol.union.core.continuation.UnionItemContinuation
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.CollectionContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController(
    private val router: BlockchainRouter<CollectionService>,
    private val apiService: CollectionApiService
) : CollectionControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val safeSize = PageSize.COLLECTION.limit(size)
        val slices = apiService.getAllCollections(blockchains, continuation, safeSize)
        val total = slices.sumOf { it.page.total }
        val arg = ArgPaging(CollectionContinuation.ById, slices.map { it.toSlice() }).getSlice(safeSize)

        logger.info("Response for getAllCollections(blockchains={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, arg.entities.size, total,
            arg.continuation, slices.map { it.page.entities.size }
        )
        val result = toDto(arg, total)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<CollectionDto> {
        val collectionId = IdParser.parseCollectionId(collection)
        val result = router.getService(collectionId.blockchain).getCollectionById(collectionId.value)
        return ResponseEntity.ok(result)
    }

    override suspend fun refreshCollectionMeta(collection: String): ResponseEntity<Unit> {
        // TODO[meta]: fetch all items by collection and schedule meta update for all of them.
        val collectionId = IdParser.parseCollectionId(collection)
        logger.info("Refreshing collection meta for '{}'", collection)
        router.getService(collectionId.blockchain).refreshCollectionMeta(collectionId.value)
        return ResponseEntity.ok().build()
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val safeSize = PageSize.COLLECTION.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val filter = BlockchainFilter(blockchains)
        val blockchainPages = router.executeForAll(filter.exclude(ownerAddress.blockchainGroup)) {
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

    private fun toDto(page: Slice<CollectionDto>, total: Long): CollectionsDto {
        return CollectionsDto(
            total = total,
            continuation = page.continuation,
            collections = page.entities
        )
    }

}
