package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

object ShortItemConverter {

    fun convert(item: UnionItemDto): ShortItem {
        return ShortItem(
            blockchain = item.id.blockchain,
            token = item.id.token.value,
            tokenId = item.id.tokenId,
            // Default enrichment data
            sellers = 0,
            totalStock = BigInteger.ZERO,
            bestSellOrder = null,
            bestBidOrder = null
        )
    }
}
