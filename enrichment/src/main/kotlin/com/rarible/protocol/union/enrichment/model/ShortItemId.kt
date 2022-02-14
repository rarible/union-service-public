package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto

data class ShortItemId(
    val blockchain: BlockchainDto,
    val itemId: String
) {

    constructor(dto: ItemIdDto) : this(
        dto.blockchain,
        dto.value
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): ItemIdDto {
        return ItemIdDto(
            blockchain = blockchain,
            value = itemId
        )
    }

}