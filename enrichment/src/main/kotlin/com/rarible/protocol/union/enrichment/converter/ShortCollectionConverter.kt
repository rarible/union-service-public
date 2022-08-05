package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.ShortCollection

object ShortCollectionConverter {

    // TODO Refactor me. This method must be in test package
    fun convert(collection: UnionCollection, statistics: CollectionStatistics?): ShortCollection {
        return ShortCollection(
            blockchain = collection.id.blockchain,
            collectionId = collection.id.value,
            statistics = statistics,
            // Default enrichment data
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            lastUpdatedAt = nowMillis()
        )
    }
}
