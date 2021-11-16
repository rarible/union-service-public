package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership

object EnrichedOwnershipConverter {

    fun convert(
        ownership: UnionOwnership,
        shortOwnership: ShortOwnership? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap()
    ): OwnershipDto {
        return OwnershipDto(
            id = ownership.id,
            blockchain = ownership.id.blockchain,
            contract = ContractAddress(ownership.id.blockchain, ownership.id.contract),
            tokenId = ownership.id.tokenId,
            owner = ownership.id.owner,
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