package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.enrichment.model.ShortCollection

object ShortCollectionConverter {

    fun convert(item: UnionCollection): ShortCollection {
        return ShortCollection(
            blockchain = item.id.blockchain,
            collectionId = item.id.value,
            // Default enrichment data
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            lastUpdatedAt = nowMillis()
        )
    }

}