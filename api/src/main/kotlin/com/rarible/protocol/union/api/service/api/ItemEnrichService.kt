package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import org.springframework.stereotype.Service

@Service
class ItemEnrichService(
    private val enrichmentItemService: EnrichmentItemService
) {

    private val metaPipeline = ItemMetaPipeline.API

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

    suspend fun enrich(unionItems: List<UnionItem>): List<ItemDto> {
        return enrichmentItemService.enrichItems(unionItems, metaPipeline)
    }
}
