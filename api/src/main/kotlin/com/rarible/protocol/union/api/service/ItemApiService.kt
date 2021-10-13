package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class ItemApiService(
    private val orderApiService: OrderApiService,
    private val enrichmentItemService: EnrichmentItemService
) {

    suspend fun enrich(unionItemsPage: Page<UnionItemDto>): ItemsDto {
        return ItemsDto(
            total = unionItemsPage.total,
            continuation = unionItemsPage.continuation,
            items = enrich(unionItemsPage.entities)
        )
    }

    suspend fun enrich(unionItem: UnionItemDto): ItemDto {
        val shortId = ShortItemId(unionItem.id)
        val shortItem = enrichmentItemService.get(shortId)
        if (shortItem == null) {
            return EnrichedItemConverter.convert(unionItem)
        }
        return enrichmentItemService.enrichItem(shortItem, unionItem)
    }

    private suspend fun enrich(unionItems: List<UnionItemDto>): List<ItemDto> {
        if (unionItems.isEmpty()) {
            return emptyList()
        }
        val existingEnrichedItems: Map<ItemIdDto, ShortItem> = enrichmentItemService
            .findAll(unionItems.map { ShortItemId(it.id) })
            .associateBy { it.id.toDto() }

        // Looking for full orders for existing items in order-indexer
        val shortOrderIds = existingEnrichedItems.values
            .map { listOfNotNull(it.bestBidOrder?.dtoId, it.bestSellOrder?.dtoId) }
            .flatten()

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        val result = unionItems.map {
            val existingEnrichedItem = existingEnrichedItems[it.id]
            EnrichedItemConverter.convert(it, existingEnrichedItem, orders)
        }

        return result
    }
}