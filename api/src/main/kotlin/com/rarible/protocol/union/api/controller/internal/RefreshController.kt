package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RefreshController(
    private val refreshService: EnrichmentRefreshService
) {

    @PostMapping(
        value = ["/v0.1/refresh/item/{itemId}/refresh"],
        produces = ["application/json"]
    )
    suspend fun reconcileItem(
        @PathVariable("itemId") itemId: String
    ): ResponseEntity<ItemDto> {
        val unionItemId = ItemIdParser.parseFull(itemId)
        val result = refreshService.refreshItem(unionItemId)
        return ResponseEntity.ok(result)
    }

    @PostMapping(
        value = ["/v0.1/refresh/item/{itemId}/reconcile"],
        produces = ["application/json"]
    )
    suspend fun reconcileItem(
        @PathVariable("itemId") itemId: String,
        @RequestParam(value = "full", required = false, defaultValue = "false") full: Boolean
    ): ResponseEntity<ItemDto> {
        val unionItemId = ItemIdParser.parseFull(itemId)
        val result = refreshService.reconcileItem(unionItemId, full)
        return ResponseEntity.ok(result)
    }

    @PostMapping(
        value = ["/v0.1/refresh/ownership/{ownershipId}/refresh"],
        produces = ["application/json"]
    )
    suspend fun recalculateOwnership(
        @PathVariable("ownershipId") ownershipId: String
    ): ResponseEntity<OwnershipDto> {
        val unionOwnershipId = OwnershipIdParser.parseFull(ownershipId)
        val result = refreshService.refreshOwnership(unionOwnershipId)
        return ResponseEntity.ok(result)
    }

    @PostMapping(
        value = ["/v0.1/refresh/ownership/{ownershipId}/reconcile"],
        produces = ["application/json"]
    )
    suspend fun reconcileOwnership(
        @PathVariable("ownershipId") ownershipId: String
    ): ResponseEntity<OwnershipDto> {
        val unionOwnershipId = OwnershipIdParser.parseFull(ownershipId)
        val result = refreshService.reconcileOwnership(unionOwnershipId)
        return ResponseEntity.ok(result)
    }

}