package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import java.math.BigInteger

data class ShortItemId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger
) {

    constructor(dto: ItemIdDto) : this(
        dto.blockchain,
        dto.contract,
        dto.tokenId
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): ItemIdDto {
        return ItemIdDto(
            blockchain = blockchain,
            contract = token,
            tokenId = tokenId
        )
    }

}