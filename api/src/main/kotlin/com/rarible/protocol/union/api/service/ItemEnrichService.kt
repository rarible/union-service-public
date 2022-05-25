package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.api.OrderApiService
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.meta.UnionMetaService
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

@ExperimentalCoroutinesApi
@Service
class ItemEnrichService(
    private val unionMetaService: UnionMetaService,
    private val enrichmentItemService: EnrichmentItemService,
    private val orderApiService: OrderApiService,
) {

    companion object {
        private val logger by Logger()
    }

    suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto {
        return ItemsDto(
            total = unionItemsPage.total,
            continuation = unionItemsPage.continuation,
            items = enrich(unionItemsPage.entities)
        )
    }

    suspend fun enrich(unionItemsSlice: Slice<UnionItem>, total: Long): ItemsDto {
        return ItemsDto(
            total = total,
            continuation = unionItemsSlice.continuation,
            items = enrich(unionItemsSlice.entities)
        )
    }

    suspend fun enrich(unionItem: UnionItem): ItemDto {
        val shortId = ShortItemId(unionItem.id)
        val shortItem = enrichmentItemService.get(shortId)
        return enrichmentItemService.enrichItem(shortItem, unionItem)
    }

    suspend fun enrich(unionItems: List<UnionItem>, continuation: String?, total: Long?): ItemsDto =
        ItemsDto(
            total = total,
            continuation = continuation,
            items = enrich(unionItems)
        )

    private suspend fun enrich(unionItems: List<UnionItem>): List<ItemDto> {
        if (unionItems.isEmpty()) {
            return emptyList()
        }
        val now = nowMillis()

        val enrichedItems = coroutineScope {

            val meta = async {
                unionMetaService.getAvailableMeta(unionItems.map { it.id })
            }

            val shortItems: Map<ItemIdDto, ShortItem> = enrichmentItemService
                .findAll(unionItems.map { ShortItemId(it.id) })
                .associateBy { it.id.toDto() }

            // Looking for full orders for existing items in order-indexer
            val shortOrderIds = shortItems.values
                .map { it.getAllBestOrders() }
                .flatten()
                .map { it.dtoId }

            val orders = orderApiService.getByIds(shortOrderIds)
                .associateBy { it.id }

            val enriched = unionItems.map {
                val shortItem = shortItems[it.id]
                enrichmentItemService.enrichItem(
                    shortItem = shortItem,
                    item = it,
                    orders = orders,
                    meta = meta.await()
                )
            }
            logger.info("Enriched {} of {} Items ({}ms)", shortItems.size, unionItems.size, spent(now))
            enriched
        }


        return enrichedItems
    }
}