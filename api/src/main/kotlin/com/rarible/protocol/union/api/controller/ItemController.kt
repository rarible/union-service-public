package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.ItemApiService
import com.rarible.protocol.union.api.service.OwnershipApiService
import com.rarible.protocol.union.api.util.BlockchainFilter
import com.rarible.protocol.union.core.continuation.UnionItemContinuation
import com.rarible.protocol.union.core.converter.ItemOwnershipConverter
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.RestrictionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdsDto
import com.rarible.protocol.union.dto.ItemWithOwnershipDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionCheckResultDto
import com.rarible.protocol.union.dto.RoyaltiesDto
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.UnionMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration

@ExperimentalCoroutinesApi
@RestController
class ItemController(
    private val itemApiService: ItemApiService,
    private val ownershipApiService: OwnershipApiService,
    private val router: BlockchainRouter<ItemService>,
    private val enrichmentItemService: EnrichmentItemService,
    private val unionMetaService: UnionMetaService,
    private val restrictionService: RestrictionService,
    private val unionMetaProperties: UnionMetaProperties
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
        val slices = itemApiService.getAllItems(blockchains, continuation, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        val total = slices.map { it.page.total }.sum()
        val arg = ArgPaging(UnionItemContinuation.ByLastUpdatedAndId, slices.map { it.toSlice() }).getSlice(safeSize)

        logger.info("Response for getAllItems(blockchains={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, arg.entities.size, total,
            arg.continuation, slices.map { it.page.entities.size }
        )

        val result = itemApiService.enrich(arg, total)
        return ResponseEntity.ok(result)
    }

    @GetMapping(value = ["/v0.1/items/{itemId}/animation"])
    suspend fun getItemAnimationById(@PathVariable("itemId") itemId: String): ResponseEntity<Resource> {
        val meta = getAvailableMetaOrLoadSynchronously(itemId)
        val unionMetaContent = meta.content
            .find { it.properties is UnionVideoProperties && it.representation == MetaContentDto.Representation.ORIGINAL }
            ?: throw UnionNotFoundException("No animation found for item $itemId")
        return createRedirectResponse(unionMetaContent)
    }

    @GetMapping(value = ["/v0.1/items/{itemId}/image"])
    suspend fun getItemImageById(@PathVariable("itemId") itemId: String): ResponseEntity<Resource> {
        val meta = getAvailableMetaOrLoadSynchronously(itemId)
        val unionMetaContent = meta.content
            .find { it.properties is UnionImageProperties && it.representation == MetaContentDto.Representation.ORIGINAL }
            ?: throw UnionNotFoundException("No image found for item $itemId")
        return createRedirectResponse(unionMetaContent)
    }

    private suspend fun getAvailableMetaOrLoadSynchronously(itemId: String): UnionMeta {
        return try {
            withTimeout(timeoutSyncLoadingMeta) {
                unionMetaService.getAvailableMetaOrLoadSynchronously(
                    itemId = IdParser.parseItemId(itemId),
                    synchronous = true
                )
            }
        } catch (e: CancellationException) {
            logger.warn("Timeout synchronously load meta for $itemId", e)
            null
        } catch (e: Exception) {
            logger.error("Cannot synchronously load meta for $itemId", e)
            null
        } ?: throw UnionNotFoundException("Meta for $itemId is not found")
    }

    override suspend fun getItemById(
        itemId: String
    ): ResponseEntity<ItemDto> {
        val fullItemId = IdParser.parseItemId(itemId)
        val shortItemId = ShortItemId(fullItemId)
        val unionItem = enrichmentItemService.fetch(shortItemId)
        val shortItem = enrichmentItemService.get(shortItemId)
        val enrichedUnionItem = enrichmentItemService.enrichItem(
            shortItem = shortItem,
            item = unionItem,
            loadMetaSynchronously = true
        )
        return ResponseEntity.ok(enrichedUnionItem)
    }

    override suspend fun getItemByIds(itemIdsDto: ItemIdsDto): ResponseEntity<ItemsDto> {
        val items = itemApiService.getItemsByIds(itemIdsDto.ids)
        return ResponseEntity.ok(ItemsDto(items = items, total = items.size.toLong()))
    }

    override suspend fun getItemRoyaltiesById(
        itemId: String
    ): ResponseEntity<RoyaltiesDto> {
        val fullItemId = IdParser.parseItemId(itemId)
        val royalties = router.getService(fullItemId.blockchain).getItemRoyaltiesById(fullItemId.value)
        return ResponseEntity.ok(RoyaltiesDto(royalties))
    }

    override suspend fun checkItemRestriction(
        itemId: String,
        restrictionCheckFormDto: RestrictionCheckFormDto
    ): ResponseEntity<RestrictionCheckResultDto> {
        val fullItemId = IdParser.parseItemId(itemId)
        val checkResult = restrictionService.checkRestriction(fullItemId, restrictionCheckFormDto)
        val dto = RestrictionCheckResultDto(
            success = checkResult.success,
            message = checkResult.message
        )
        return ResponseEntity.ok(dto)
    }

    override suspend fun resetItemMeta(itemId: String): ResponseEntity<Unit> {
        val fullItemId = IdParser.parseItemId(itemId)
        // TODO[meta]: when all Blockchains stop caching the meta, we can remove this endpoint call.
        logger.info("Refreshing item meta for $itemId")
        router.getService(fullItemId.blockchain).resetItemMeta(fullItemId.value)
        unionMetaService.scheduleLoading(fullItemId)
        return ResponseEntity.ok().build()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val collectionId = IdParser.parseCollectionId(collection)
        val result = router.getService(collectionId.blockchain)
            .getItemsByCollection(collectionId.value, null, continuation, safeSize)

        logger.info(
            "Response for getItemsByCollection(collection={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={})",
            collection, continuation, size, result.entities.size, result.total, result.continuation
        )

        val enriched = itemApiService.enrich(result)
        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val creatorAddress = IdParser.parseAddress(creator)
        val filter = BlockchainFilter(blockchains)

        val blockchainPages = router.executeForAll(filter.exclude(creatorAddress.blockchainGroup)) {
            it.getItemsByCreator(creatorAddress.value, continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            UnionItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getItemsByCreator(creator={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            creator, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation,
            blockchainPages.map { it.entities.size }
        )

        val enriched = itemApiService.enrich(combinedPage)
        return ResponseEntity.ok(enriched)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val filter = BlockchainFilter(blockchains)
        val blockchainPages = router.executeForAll(filter.exclude(ownerAddress.blockchainGroup)) {
            it.getItemsByOwner(ownerAddress.value, continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            UnionItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getItemsByOwner(owner={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            owner, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation,
            blockchainPages.map { it.entities.size }
        )

        val enriched = itemApiService.enrich(combinedPage)
        return ResponseEntity.ok(enriched)
    }

    private fun createRedirectResponse(unionMetaContent: UnionMetaContent): ResponseEntity<Resource> {
        val httpHeaders = HttpHeaders()
        httpHeaders.location = URI(unionMetaContent.url)
        return ResponseEntity(httpHeaders, HttpStatus.TEMPORARY_REDIRECT)
    }

    override suspend fun getItemsByOwnerWithOwnership(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsWithOwnershipDto> {
        val safeSize = PageSize.ITEM.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val page = ownershipApiService.getOwnershipByOwner(ownerAddress, continuation, safeSize)
        val ids = page.entities.map { it.id.getItemId() }
        val items = router.executeForAll(ownerAddress.blockchainGroup.subchains()) {
            it.getItemsByIds(ids.map { it.value })
        }.flatten().associateBy { it.id }

        val wrapped = page.entities.map {
            val item = items[it.id.getItemId()]
            coroutineScope {
                async {
                    if (null != item) {
                        ItemWithOwnershipDto(
                            itemApiService.enrich(item), ItemOwnershipConverter.convert(it)
                        )
                    } else {
                        logger.warn("Item for ${it.id} ownership wasn't found")
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()

        return ResponseEntity.ok(ItemsWithOwnershipDto(wrapped.size.toLong(), page.continuation, wrapped))
    }

    private companion object {
        // A timeout to avoid infinite meta loading.
        val timeoutSyncLoadingMeta: Duration = Duration.ofSeconds(30)
    }
}
