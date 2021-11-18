package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import java.time.Instant

object ShortOwnershipConverter {

    fun convert(dto: UnionOwnership): ShortOwnership {
        return ShortOwnership(
            blockchain = dto.id.blockchain,
            token = dto.id.contract,
            tokenId = dto.id.tokenId,
            owner = dto.id.owner.value,
            // Default enrichment data
            bestSellOrders = emptyMap(),
            bestSellOrder = null,
            lastUpdatedAt = Instant.EPOCH
        )
    }
}
