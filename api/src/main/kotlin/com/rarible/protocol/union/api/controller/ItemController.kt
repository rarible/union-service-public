package com.rarible.protocol.union.api.controller

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
        lastUpdatedTo: Long?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllItems(continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo, includeMeta)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = ContinuationPaging(
            UnionItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.items }
        ).getPage(size)

        val result = UnionItemsDto(total, combinedPage.printContinuation(), combinedPage.entities)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemById(
        itemId: String,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemDto> {
        val (blockchain, shortItemId) = IdParser.parse(itemId)
        val result = router.getService(blockchain).getItemById(shortItemId, includeMeta)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        val (blockchain, shortCollection) = IdParser.parse(collection)
        val result = router.getService(blockchain)
            .getItemsByCollection(shortCollection, continuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        val (blockchain, shortCreator) = IdParser.parse(creator)
        val result = router.getService(blockchain).getItemsByCreator(shortCreator, continuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        val (blockchain, shortOwner) = IdParser.parse(owner)
        val result = router.getService(blockchain).getItemsByOwner(shortOwner, continuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }
}