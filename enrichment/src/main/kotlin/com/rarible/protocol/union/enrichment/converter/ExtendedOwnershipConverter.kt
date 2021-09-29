package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership

object ExtendedOwnershipConverter {

    fun convert(
        ownership: UnionOwnershipDto,
        shortOwnership: ShortOwnership? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap()
    ): OwnershipDto {
        return OwnershipDto(
            id = ownership.id,
            contract = ownership.contract,
            tokenId = ownership.tokenId,
            owner = ownership.owner,
            creators = ownership.creators,
            value = ownership.value,
            lazyValue = ownership.lazyValue,
            createdAt = ownership.createdAt,
            pending = ownership.pending,
            // Enrichment data
            bestSellOrder = shortOwnership?.bestSellOrder?.let { orders[it.dtoId] }
        )
    }
}