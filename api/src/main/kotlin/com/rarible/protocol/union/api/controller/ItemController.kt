package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.service.ItemApiService
import com.rarible.protocol.union.core.continuation.Paging
import com.rarible.protocol.union.core.service.ItemServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.ItemContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class ItemController(
    private val itemApiService: ItemApiService,
    private val router: ItemServiceRouter
) : ItemControllerApi {

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllItems(continuation, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            ItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        val result = itemApiService.enrich(combinedPage)

        return ResponseEntity.ok(result)
    }

    override suspend fun getItemById(
        itemId: String
    ): ResponseEntity<ItemDto> {
        val (blockchain, shortItemId) = IdParser.parse(itemId)
        val result = router.getService(blockchain).getItemById(shortItemId)
        val enriched = itemApiService.enrich(result)
        return ResponseEntity.ok(enriched)
    }

    override suspend fun resetItemMeta(itemId: String): ResponseEntity<Unit> {
        val (blockchain, shortItemId) = IdParser.parse(itemId)
        router.getService(blockchain).resetItemMeta(shortItemId)
        return ResponseEntity.ok().build()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val (blockchain, shortCollection) = IdParser.parse(collection)
        val result = router.getService(blockchain)
            .getItemsByCollection(shortCollection, continuation, safeSize)

        val enriched = itemApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val (blockchain, shortCreator) = IdParser.parse(creator)
        val result = router.getService(blockchain).getItemsByCreator(shortCreator, continuation, safeSize)

        val enriched = itemApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val (blockchain, shortOwner) = IdParser.parse(owner)
        val result = router.getService(blockchain).getItemsByOwner(shortOwner, continuation, safeSize)

        val enriched = itemApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

}