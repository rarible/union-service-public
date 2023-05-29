package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.api.dto.HookEventType
import com.rarible.protocol.union.api.dto.SimpleHashItemIdDeserializer
import com.rarible.protocol.union.api.dto.SimpleHashNftMetadataUpdateDto
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionMigrator
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RefreshController(
    private val refreshService: EnrichmentRefreshService,
    private val customCollectionMigrator: CustomCollectionMigrator,
    private val router: BlockchainRouter<ItemService>,
    private val itemMetaService: ItemMetaService,
) {
    @PostMapping(
        value = ["/v0.1/refresh/collection/{collectionId}/reconcile"],
        produces = ["application/json"]
    )
    suspend fun reconcileCollection(
        @PathVariable("collectionId") collectionId: String
    ): ResponseEntity<CollectionEventDto> {
        val unionCollectionId = IdParser.parseCollectionId(collectionId)
        val result = refreshService.reconcileCollection(unionCollectionId)
        return ResponseEntity.ok(result)
    }

    @PostMapping(
        value = ["/v0.1/refresh/item/{itemId}/reconcile"],
        produces = ["application/json"]
    )
    suspend fun reconcileItem(
        @PathVariable("itemId") itemId: String,
        @RequestParam(value = "full", required = false, defaultValue = "false") full: Boolean
    ): ResponseEntity<ItemEventDto> {
        val unionItemId = IdParser.parseItemId(itemId)
        val result = refreshService.reconcileItem(unionItemId, full)
        return ResponseEntity.ok(result)
    }

    @PostMapping(
        value = ["/v0.1/refresh/ownership/{ownershipId}/reconcile"],
        produces = ["application/json"]
    )
    suspend fun reconcileOwnership(
        @PathVariable("ownershipId") ownershipId: String
    ): ResponseEntity<OwnershipEventDto> {
        val unionOwnershipId = OwnershipIdParser.parseFull(ownershipId)
        val result = refreshService.reconcileOwnership(unionOwnershipId)
            ?: throw UnionNotFoundException("Ownership $ownershipId not found")
        return ResponseEntity.ok(result)
    }

    @PostMapping(
        value = ["/v0.1/refresh/item/{itemId}/migrate/collection"],
        produces = ["application/json"]
    )
    suspend fun migrateCustomCollection(
        @PathVariable("itemId") itemId: String
    ): ResponseEntity<Unit> {
        val unionItemId = IdParser.parseItemId(itemId)
        customCollectionMigrator.migrate(
            listOf(
                router.getService(unionItemId.blockchain).getItemById(unionItemId.value)
            )
        )

        return ResponseEntity.noContent().build()
    }


    /**
     * https://docs.simplehash.com/reference/webhook-events
     *
     * To indicate that a webhook message has been processed, return a HTTP 2XX status code (200-299)
     * to the webhook message within a reasonable time-frame (15 seconds).
     */
    @PostMapping(value = ["/v0.1/refresh/items/simplehash/metaUpdateWebhook"])
    suspend fun simpleHashMetaUpdateWebhook(
        @RequestBody update: SimpleHashNftMetadataUpdateDto
    ): ResponseEntity<Unit> {
        when (update.type) {
            is HookEventType.ChainNftMetadataUpdate -> {
                update.nfts.forEach { nft ->
                    scheduleWebHookMetaRefresh(nft.itemId)
                }
            }
            is HookEventType.Unknown -> {
                logger.warn("Unknown webhook event type: ${update.type.value}")
            }
        }
        return ResponseEntity.noContent().build()
    }

    private suspend fun scheduleWebHookMetaRefresh(itemId: String) {
        try {
            val itemIdDto = SimpleHashItemIdDeserializer.parse(itemId)
            itemMetaService.schedule(itemIdDto, ItemMetaPipeline.API, false)
        } catch (e: Exception) {
            logger.error("Error processing webhook event for item $itemId", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RefreshController::class.java)
    }
}