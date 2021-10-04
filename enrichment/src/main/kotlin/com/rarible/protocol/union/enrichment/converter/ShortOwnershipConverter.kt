package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership

object ShortOwnershipConverter {

    fun convert(dto: UnionOwnershipDto): ShortOwnership {
        return ShortOwnership(
            blockchain = dto.id.blockchain,
            token = dto.id.token.value,
            tokenId = dto.id.tokenId,
            owner = dto.id.owner.value,
            // Default enrichment data
            bestSellOrder = null
        )
    }
}