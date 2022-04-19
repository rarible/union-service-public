package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.OwnershipDto

object EsOwnershipConverter {
    fun convert(source: OwnershipDto): EsOwnership = EsOwnership(
        ownershipId = source.id.fullId(),
        blockchain = source.blockchain,
        itemId = source.itemId?.fullId(),
        collection = source.collection?.fullId(),
        value = source.value.toString(),
        date = source.createdAt,
    )
}
