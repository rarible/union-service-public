package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

@Deprecated("Should be replaced by implementation without token/tokenId")
object ShortItemConverter {

    fun convert(item: UnionItem): ShortItem {
        val (contract, tokenId) = CompositeItemIdParser.split(item.id.value)
        return ShortItem(
            blockchain = item.id.blockchain,
            token = contract,
            tokenId = tokenId,
            // Default enrichment data
            sellers = 0,
            totalStock = BigInteger.ZERO,
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            auctions = emptySet(),
            lastUpdatedAt = nowMillis()
        )
    }
}
