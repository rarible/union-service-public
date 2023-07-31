package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId

object EnrichmentCollectionConverter {

    fun convert(collection: UnionCollection): EnrichmentCollection {
        return EnrichmentCollection(
            blockchain = collection.id.blockchain,
            collectionId = collection.id.value,

            name = collection.name,
            status = collection.status,
            type = collection.type,
            minters = collection.minters,
            features = collection.features,
            owner = collection.owner,
            parent = collection.parent?.let { EnrichmentCollectionId(it) },
            symbol = collection.symbol,
            self = collection.self,

            // Default enrichment data
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            originOrders = emptySet(),
            metaEntry = null,
            lastUpdatedAt = nowMillis(),
        )
    }
}
