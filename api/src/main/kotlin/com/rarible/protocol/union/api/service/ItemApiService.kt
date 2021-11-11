package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class ItemApiService(
    private val orderApiService: OrderApiService,
    private val enrichmentItemService: EnrichmentItemService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto {
        return ItemsDto(
            total = unionItemsPage.total,
            continuation = unionItemsPage.continuation,
            items = enrich(unionItemsPage.entities)
        )
    }

    suspend fun enrich(unionItem: UnionItem): ItemDto {
        val shortId = ShortItemId(unionItem.id)
        val shortItem = enrichmentItemService.get(shortId)
        return enrichmentItemService.enrichItem(shortItem, unionItem)
    }

    private suspend fun enrich(unionItems: List<UnionItem>): List<ItemDto> {
        if (unionItems.isEmpty()) {
            return emptyList()
        }
        val now = nowMillis()
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
            enrichmentItemService.enrichItem(existingEnrichedItem, it, orders)
        }

        logger.info(
            "Enriched {} of {} Items, {} Orders fetched ({}ms)",
            existingEnrichedItems.size, result.size, orders.size, spent(now)
        )

        return result
    }
}
