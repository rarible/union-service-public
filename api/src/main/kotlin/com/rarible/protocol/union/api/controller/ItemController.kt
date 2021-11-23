package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.ItemApiService
import com.rarible.protocol.union.core.continuation.ItemContinuation
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.RestrictionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.RestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionCheckResultDto
import com.rarible.protocol.union.dto.RoyaltiesDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class ItemController(
    private val itemApiService: ItemApiService,
    private val router: BlockchainRouter<ItemService>,
    private val enrichmentMetaService: EnrichmentMetaService,
    private val restrictionService: RestrictionService
) : ItemControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

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

        logger.info("Response for getAllItems(blockchains={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, combinedPage.entities.size, combinedPage.total,
            combinedPage.continuation, blockchainPages.map { it.entities.size }
        )

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

    override suspend fun getItemRoyaltiesById(
        itemId: String
    ): ResponseEntity<RoyaltiesDto> {
        val fullItemId = ItemIdParser.parseFull(itemId)
        val royalties = router.getService(fullItemId.blockchain).getItemRoyaltiesById(fullItemId.value)
        return ResponseEntity.ok(RoyaltiesDto(royalties))
    }

    override suspend fun checkItemRestriction(
        itemId: String,
        restrictionCheckFormDto: RestrictionCheckFormDto
    ): ResponseEntity<RestrictionCheckResultDto> {
        val fullItemId = ItemIdParser.parseFull(itemId)
        val checkResult = restrictionService.checkRestriction(fullItemId, restrictionCheckFormDto)
        val dto = RestrictionCheckResultDto(
            success = checkResult.success,
            message = checkResult.message
        )
        return ResponseEntity.ok(dto)
    }

    override suspend fun resetItemMeta(itemId: String): ResponseEntity<Unit> {
        val fullItemId = ItemIdParser.parseFull(itemId)
        enrichmentMetaService.resetMeta(fullItemId)
        router.getService(fullItemId.blockchain).resetItemMeta(fullItemId.value)

        logger.info("Item meta has been reset: {}", itemId)
        return ResponseEntity.ok().build()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val collectionAddress = IdParser.parseContract(collection)
        val result = router.getService(collectionAddress.blockchain)
            .getItemsByCollection(collectionAddress.value, continuation, safeSize)

        logger.info(
            "Response for getItemsByCollection(collection={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            collection, continuation, size, result.entities.size, result.total, result.continuation
        )

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

        val blockchainPages = router.executeForAll(creatorAddress.blockchainGroup.subchains()) {
            it.getItemsByCreator(creatorAddress.value, continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            ItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getItemsByCreator(creator={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            creator, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation
        )

        val enriched = itemApiService.enrich(combinedPage)
        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val blockchainPages = router.executeForAll(ownerAddress.blockchainGroup.subchains()) {
            it.getItemsByOwner(ownerAddress.value, continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            ItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getItemsByCreator(owner={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            owner, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation
        )

        val enriched = itemApiService.enrich(combinedPage)
        return ResponseEntity.ok(enriched)
    }

}