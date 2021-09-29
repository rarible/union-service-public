package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

object ShortItemConverter {

    fun convert(item: ItemDto): ShortItem {
        return ShortItem(
            blockchain = item.id.blockchain,
            token = item.id.token.value,
            tokenId = item.id.tokenId,
            // Default enrichment data, should be replaced out of this converter
            sellers = 0,
            totalStock = BigInteger.ZERO,
            bestSellOrder = null,
            bestBidOrder = null,
            unlockable = false
        )
    }
}
