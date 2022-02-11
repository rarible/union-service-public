package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import java.math.BigInteger

@Deprecated("Should be replaced by implementation without token/tokenId")
data class ShortItemId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger
) {

    constructor(dto: ItemIdDto) : this(
        dto.blockchain,
        // TODO won't work with Solana
        dto.value.substringBefore(IdParser.DELIMITER),
        BigInteger(dto.value.substringAfter(IdParser.DELIMITER))
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): ItemIdDto {
        return ItemIdDto(
            blockchain = blockchain,
            value = "${token}:${tokenId}"
        )
    }

}