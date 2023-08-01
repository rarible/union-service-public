package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.ItemSourceSelectService
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
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
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ItemsDto {
        if (size !in 1..200) throw UnionException("Size param must be between 1 and 200")

        val (from, id) = if (continuation?.contains("_") == true) {
            val dateIdContinuation = DateIdContinuation.parse(continuation)
            dateIdContinuation?.date to dateIdContinuation?.id
        } else {
            null to continuation
        }
        val ids = itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = from ?: lastUpdatedFrom,
            lastUpdatedTo = lastUpdatedTo,
            fromId = id?.let { ShortItemId(IdParser.parseItemId(id)) },
            size = size
        ).filter { it.date <= lastUpdatedTo }

        if (ids.isEmpty()) {
            return ItemsDto()
        }
        val items = itemSourceSelectService.getItemsByIds(ids.map { it.id.toDto() })
        val next = if (ids.size == size) {
            val last = ids.last()
            DateIdContinuation(last.date, last.id.toDto().fullId())
        } else {
            null
        }
        return ItemsDto(
            total = 0,
            items = items,
            continuation = next?.toString()
        )
    }
}
