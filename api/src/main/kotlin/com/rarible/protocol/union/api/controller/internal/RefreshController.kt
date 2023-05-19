package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionMigrator
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RefreshController(
    private val refreshService: EnrichmentRefreshService,
    private val customCollectionMigrator: CustomCollectionMigrator,
    private val router: BlockchainRouter<ItemService>
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

}