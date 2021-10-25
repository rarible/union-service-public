package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.ItemApiService
import com.rarible.protocol.union.core.continuation.ItemContinuation
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class ItemController(
    private val itemApiService: ItemApiService,
    private val router: BlockchainRouter<ItemService>,
    private val enrichmentMetaService: EnrichmentMetaService
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
        val fullItemId = ItemIdParser.parseFull(itemId)
        val result = router.getService(fullItemId.blockchain).getItemById(fullItemId.value)
        val enriched = itemApiService.enrich(result)
        return ResponseEntity.ok(enriched)
    }

    override suspend fun resetItemMeta(itemId: String): ResponseEntity<Unit> {
        val fullItemId = ItemIdParser.parseFull(itemId)
        enrichmentMetaService.resetMeta(fullItemId)
        router.getService(fullItemId.blockchain).resetItemMeta(fullItemId.value)
        return ResponseEntity.ok().build()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val collectionAddress = IdParser.parseAddress(collection)
        val result = router.getService(collectionAddress.blockchain)
            .getItemsByCollection(collectionAddress.value, continuation, safeSize)

        val enriched = itemApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val creatorAddress = IdParser.parseAddress(creator)
        val result = router.getService(creatorAddress.blockchain)
            .getItemsByCreator(creatorAddress.value, continuation, safeSize)

        val enriched = itemApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val result = router.getService(ownerAddress.blockchain)
            .getItemsByOwner(ownerAddress.value, continuation, safeSize)

        val enriched = itemApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

}