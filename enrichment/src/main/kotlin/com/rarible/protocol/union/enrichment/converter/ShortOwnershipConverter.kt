package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import java.time.Instant

object ShortOwnershipConverter {

    fun convert(ownership: UnionOwnership): ShortOwnership {
        return ShortOwnership(
            blockchain = ownership.id.blockchain,
            itemId = ownership.id.itemIdValue,
            owner = ownership.id.owner.value,
            // Default enrichment data
            bestSellOrders = emptyMap(),
            bestSellOrder = null,
            lastUpdatedAt = Instant.EPOCH
        )
    }
}
