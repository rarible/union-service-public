package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

object ShortItemConverter {

    fun convert(item: UnionItem): ShortItem {
        return ShortItem(
            blockchain = item.id.blockchain,
            itemId = item.id.value,
            // Default enrichment data
            sellers = 0,
            totalStock = BigInteger.ZERO,
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            auctions = emptySet(),
            lastSale = null,
            lastUpdatedAt = nowMillis()
        )
    }
}
