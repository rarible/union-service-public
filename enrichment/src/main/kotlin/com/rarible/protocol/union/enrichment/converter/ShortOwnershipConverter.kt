package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership

object ShortOwnershipConverter {

    fun convert(dto: OwnershipDto): ShortOwnership {
        return ShortOwnership(
            blockchain = dto.id.blockchain,
            token = dto.id.token.value,
            tokenId = dto.id.tokenId,
            owner = dto.id.owner.value,
            bestSellOrder = null
        )
    }
}