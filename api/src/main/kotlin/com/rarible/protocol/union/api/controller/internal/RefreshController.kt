package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
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
        value = ["/v0.1/refresh/item/{itemId}"],
        produces = ["application/json"]
    )
    suspend fun getNftOrderItemById(
        @PathVariable("itemId") itemId: String,
        @RequestParam(value = "full", required = false, defaultValue = "false") full: Boolean
    ): ResponseEntity<ItemDto> {
        val id = ItemIdParser.parseFull(itemId)
        val shortId = ShortItemId(
            blockchain = id.blockchain,
            token = id.token.value,
            tokenId = id.tokenId
        )
        val result = if (full) {
            refreshService.refreshItemWithOwnerships(shortId)
        } else {
            refreshService.refreshItem(shortId)
        }
        return ResponseEntity.ok(result)
    }

}