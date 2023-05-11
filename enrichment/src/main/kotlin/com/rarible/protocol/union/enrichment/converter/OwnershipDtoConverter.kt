package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentOwnershipData

object OwnershipDtoConverter {

    fun convert(
        ownership: UnionOwnership,
        data: EnrichmentOwnershipData = EnrichmentOwnershipData.empty()
    ): OwnershipDto {
        val shortOwnership = data.shortOwnership

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
            collection = data.customCollection ?: ownership.collection,
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
            bestSellOrder = shortOwnership?.bestSellOrder?.let { data.orders[it.dtoId] },
            originOrders = shortOwnership?.originOrders?.let { OriginOrdersConverter.convert(it, data.orders) }
                ?: emptyList(),
            source = data.shortOwnership?.source,
        )
    }
}
