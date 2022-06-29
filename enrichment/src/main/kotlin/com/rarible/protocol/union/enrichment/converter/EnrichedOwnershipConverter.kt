package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
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
        // TODO remove it later with contract/tokenId
        val contractAndTokenId = if (ownership.id.blockchain != BlockchainDto.SOLANA) {
            CompositeItemIdParser.split(ownership.id.itemIdValue)
        } else {
            null
        }
        return OwnershipDto(
            id = ownership.id,
            blockchain = ownership.id.blockchain,
            itemId = ownership.id.getItemId(),
            collection = ownership.collection,
            contract = contractAndTokenId?.let {
                ContractAddressConverter.convert(
                    ownership.id.blockchain, it.first
                )
            }, // TODO remove later
            tokenId = contractAndTokenId?.second, // TODO remove later
            owner = ownership.id.owner,
            creators = ownership.creators,
            value = ownership.value,
            lazyValue = ownership.lazyValue,
            createdAt = ownership.createdAt,
            lastUpdatedAt = ownership.lastUpdatedAt,
            pending = ownership.pending,
            // Enrichment data
            bestSellOrder = shortOwnership?.bestSellOrder?.let { orders[it.dtoId] },
            originOrders = shortOwnership?.originOrders?.let { OriginOrdersConverter.convert(it, orders) }
                ?: emptyList()
        )
    }
}
