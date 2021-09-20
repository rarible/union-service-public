package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.core.continuation.ContinuationPaging
import com.rarible.protocol.union.core.service.ItemServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.UnionItemsDto
import com.rarible.protocol.union.dto.continuation.UnionItemContinuation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ItemController(
    private val router: ItemServiceRouter
) : ItemControllerApi {

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ResponseEntity<UnionItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllItems(continuation, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = ContinuationPaging(
            UnionItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.items }
        ).getPage(safeSize)

        val result = UnionItemsDto(total, combinedPage.printContinuation(), combinedPage.entities)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemById(
        itemId: String
    ): ResponseEntity<UnionItemDto> {
        val (blockchain, shortItemId) = IdParser.parse(itemId)
        val result = router.getService(blockchain).getItemById(shortItemId)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val (blockchain, shortCollection) = IdParser.parse(collection)
        val result = router.getService(blockchain)
            .getItemsByCollection(shortCollection, continuation, safeSize)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val (blockchain, shortCreator) = IdParser.parse(creator)
        val result = router.getService(blockchain).getItemsByCreator(shortCreator, continuation, safeSize)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val (blockchain, shortOwner) = IdParser.parse(owner)
        val result = router.getService(blockchain).getItemsByOwner(shortOwner, continuation, safeSize)
        return ResponseEntity.ok(result)
    }
}