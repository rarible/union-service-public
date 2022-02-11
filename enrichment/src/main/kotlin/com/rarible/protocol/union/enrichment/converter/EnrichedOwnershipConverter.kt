package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.util.CompositeItemIdParser
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
        val (contract, tokenId) = CompositeItemIdParser.split(ownership.id.itemIdValue)
        return OwnershipDto(
            id = ownership.id,
            blockchain = ownership.id.blockchain,
            itemId = ownership.id.getItemId(),
            contract = ContractAddressConverter.convert(ownership.id.blockchain, contract), // TODO remove later
            tokenId = tokenId, // TODO remove later
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
