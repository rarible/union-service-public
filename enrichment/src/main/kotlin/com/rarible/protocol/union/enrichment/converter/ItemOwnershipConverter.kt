package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ItemOwnershipDto

object ItemOwnershipConverter {

    fun convert(ownership: UnionOwnership): ItemOwnershipDto {
        val (contract, tokenId) = CompositeItemIdParser.split(ownership.id.itemIdValue)
        return ItemOwnershipDto(
            id = ownership.id,
            blockchain = ownership.id.blockchain,
            contract = ContractAddressConverter.convert(ownership.id.blockchain, contract),
            tokenId = tokenId,
            owner = ownership.id.owner,
            creators = ownership.creators,
            value = ownership.value,
            lazyValue = ownership.lazyValue,
            createdAt = ownership.createdAt
        )
    }
}
