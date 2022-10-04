package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.ItemSourceSelectService
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ItemReconciliationController(
    private val itemRepository: ItemRepository,
    private val itemSourceSelectService: ItemSourceSelectService,
) {

    @GetMapping(value = ["/reconciliation/items"], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getItems(
        @RequestParam lastUpdatedFrom: Instant,
        @RequestParam lastUpdatedTo: Instant,
        @RequestParam(required = false) continuation: String? = null,
    ): ItemsDto {
        val ids = itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = lastUpdatedFrom,
            lastUpdatedTo = lastUpdatedTo,
            continuation = continuation?.let { ShortItemId(IdParser.parseItemId(continuation)) }
        ).map { it.toDto() }
        if (ids.isEmpty()) {
            return ItemsDto()
        }

        val items = itemSourceSelectService.getItemsByIds(ids)

        return ItemsDto(
            total = 0,
            items = items,
            continuation = ids.last().fullId()
        )
    }
}
