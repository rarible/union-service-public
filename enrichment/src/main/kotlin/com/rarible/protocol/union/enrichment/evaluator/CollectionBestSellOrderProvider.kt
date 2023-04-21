package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class CollectionBestSellOrderProvider(
    private val collectionId: EnrichmentCollectionId,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val origin: String? = null
) : BestOrderProvider<EnrichmentCollection> {

    override val entityId: String = collectionId.toString()
    override val entityType: Class<EnrichmentCollection> get() = EnrichmentCollection::class.java

    override suspend fun fetch(currencyId: String): UnionOrder? {
        return enrichmentOrderService.getBestSell(collectionId, currencyId, origin)
    }

    class Factory(
        private val collectionId: EnrichmentCollectionId,
        private val enrichmentOrderService: EnrichmentOrderService,
    ) : BestOrderProviderFactory<EnrichmentCollection> {

        override fun create(origin: String?): BestOrderProvider<EnrichmentCollection> {
            return CollectionBestSellOrderProvider(collectionId, enrichmentOrderService, origin)
        }

    }
}