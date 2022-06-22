package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.ItemSourceSelectService
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
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionCheckResultDto
import com.rarible.protocol.union.dto.RoyaltiesDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.ItemMetaService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val itemSourceSelectService: ItemSourceSelectService,
    private val router: BlockchainRouter<ItemService>,
    private val enrichmentItemService: EnrichmentItemService,
    private val itemMetaService: ItemMetaService,
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

        return ResponseEntity.ok(
            itemSourceSelectService.getAllItems(
                blockchains,
                continuation,
                size,
                showDeleted,
                lastUpdatedFrom,
                lastUpdatedTo
            )
        )
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
                itemMetaService.get(
                    itemId = IdParser.parseItemId(itemId),
                    sync = true,
                    pipeline = "default" // TODO PT-49
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
            syncMetaDownload = true
        )
        return ResponseEntity.ok(enrichedUnionItem)
    }

    override suspend fun getItemByIds(itemIdsDto: ItemIdsDto): ResponseEntity<ItemsDto> {
        val items = itemSourceSelectService.getItemsByIds(itemIdsDto.ids)
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

    override suspend fun resetItemMeta(itemId: String, sync: Boolean?): ResponseEntity<Unit> {
        // TODO: handle sync
        val fullItemId = IdParser.parseItemId(itemId)
        val safeSync = sync ?: false

        logger.info("Refreshing item meta for $itemId (sync=$safeSync)")

        // TODO[meta]: when all Blockchains stop caching the meta, we can remove this endpoint call.
        router.getService(fullItemId.blockchain).resetItemMeta(fullItemId.value)
        if (safeSync) {
            itemMetaService.download(fullItemId, "default", true)  // TODO PT-49
        } else {
            itemMetaService.schedule(fullItemId, "default", true)  // TODO PT-49
        }

        return ResponseEntity.ok().build()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {

        return ResponseEntity.ok(itemSourceSelectService.getItemsByCollection(collection, continuation, size))
    }

    override suspend fun getItemsByCreator(
        creator: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        return ResponseEntity.ok(itemSourceSelectService.getItemsByCreator(creator, blockchains, continuation, size))
    }

    override suspend fun getItemsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<ItemsDto> {
        return ResponseEntity.ok(itemSourceSelectService.getItemsByOwner(owner, blockchains, continuation, size))
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
        return ResponseEntity.ok(itemSourceSelectService.getItemsByOwnerWithOwnership(owner, continuation, size))
    }

    private companion object {
        // A timeout to avoid infinite meta loading.
        val timeoutSyncLoadingMeta: Duration = Duration.ofSeconds(30)
    }
}


