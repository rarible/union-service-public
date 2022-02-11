package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import java.time.Instant

@Deprecated("Should be replaced by implementation without token/tokenId")
object ShortOwnershipConverter {

    fun convert(ownership: UnionOwnership): ShortOwnership {
        val (contract, tokenId) = CompositeItemIdParser.split(ownership.id.itemIdValue)
        return ShortOwnership(
            blockchain = ownership.id.blockchain,
            token = contract,
            tokenId = tokenId,
            owner = ownership.id.owner.value,
            // Default enrichment data
            bestSellOrders = emptyMap(),
            bestSellOrder = null,
            lastUpdatedAt = Instant.EPOCH
        )
    }
}
