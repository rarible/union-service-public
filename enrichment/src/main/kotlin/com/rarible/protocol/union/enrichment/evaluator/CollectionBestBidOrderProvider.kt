package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class CollectionBestBidOrderProvider (
    private val collectionId: ShortCollectionId,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val origin: String? = null
) : BestOrderProvider<ShortCollection> {

    override val entityId: String = collectionId.toString()
    override val entityType: Class<ShortCollection> get() = ShortCollection::class.java

    override suspend fun fetch(currencyId: String): OrderDto? {
        return enrichmentOrderService.getBestBid(collectionId, currencyId, origin)
    }

    class Factory(
        private val collectionId: ShortCollectionId,
        private val enrichmentOrderService: EnrichmentOrderService,
    ) : BestOrderProviderFactory<ShortCollection> {

        override fun create(origin: String?): BestOrderProvider<ShortCollection> {
            return CollectionBestBidOrderProvider(collectionId, enrichmentOrderService, origin)
        }

    }
}
