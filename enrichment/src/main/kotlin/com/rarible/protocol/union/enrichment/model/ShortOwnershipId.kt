package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser

data class ShortOwnershipId(
    val blockchain: BlockchainDto,
    val itemId: String,
    val owner: String
) {

    constructor(dto: OwnershipIdDto) : this(
        dto.blockchain,
        dto.itemIdValue,
        dto.owner.value
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): OwnershipIdDto {
        return OwnershipIdDto(
            blockchain = blockchain,
            itemIdValue = itemId,
            owner = UnionAddressConverter.convert(blockchain, owner)
        )
    }

    fun getItemId(): ShortItemId {
        return ShortItemId(blockchain, itemId)
    }

    companion object {

        fun of(ownershipId: String): ShortOwnershipId {
            return ShortOwnershipId(OwnershipIdParser.parseFull(ownershipId))
        }
    }
}
