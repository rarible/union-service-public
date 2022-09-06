package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class ItemBestSellOrderProvider(
    private val item: ShortItem,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val origin: String? = null,
    private val enablePoolOrders: Boolean = true
) : BestOrderProvider<ShortItem> {

    override val entityId: String = item.id.toString()
    override val entityType: Class<ShortItem> get() = ShortItem::class.java

    override suspend fun fetch(currencyId: String): OrderDto? {
        val directOrder = enrichmentOrderService.getBestSell(item.id, currencyId, origin)
        // For origin orders we don't need to check pool orders
        if (origin != null || !enablePoolOrders) {
            return directOrder
        }

        // TODO here we rely on actual makePrice in union, but originally it might be not actual
        // Should be fine for ERC721 since there can be only one sell order for them,
        // but for 1155 it might work unstable
        val poolOrder = item.poolSellOrders
            .filter { it.currency == currencyId }
            .map { it.order }
            .reduceOrNull(BestSellOrderComparator::compare)

        return when {
            poolOrder == null -> directOrder
            directOrder == null -> enrichmentOrderService.getById(poolOrder.dtoId)
            else -> {
                val best = BestSellOrderComparator.compare(poolOrder, ShortOrderConverter.convert(directOrder))
                if (best == poolOrder) {
                    enrichmentOrderService.getById(poolOrder.dtoId) ?: directOrder // Better than nothing
                } else {
                    directOrder
                }
            }
        }
    }

    class Factory(
        private val item: ShortItem,
        private val enrichmentOrderService: EnrichmentOrderService,
        private val enablePoolOrders: Boolean
    ) : BestOrderProviderFactory<ShortItem> {

        override fun create(origin: String?): BestOrderProvider<ShortItem> {
            return ItemBestSellOrderProvider(item, enrichmentOrderService, origin, enablePoolOrders)
        }

    }
}
