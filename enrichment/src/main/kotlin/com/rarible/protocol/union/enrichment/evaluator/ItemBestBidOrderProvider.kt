package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class ItemBestBidOrderProvider(
    private val item: ShortItem,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val origin: String? = null
) : BestOrderProvider<ShortItem> {

    override val entityId: String = item.id.toString()
    override val entityType: Class<ShortItem> get() = ShortItem::class.java

    override suspend fun fetch(currencyId: String): OrderDto? {
        return enrichmentOrderService.getBestBid(item.id, currencyId, origin)
    }

    class Factory(
        private val item: ShortItem,
        private val enrichmentOrderService: EnrichmentOrderService,
    ) : BestOrderProviderFactory<ShortItem> {

        override fun create(origin: String?): BestOrderProvider<ShortItem> {
            return ItemBestBidOrderProvider(item, enrichmentOrderService, origin)
        }

    }
}
