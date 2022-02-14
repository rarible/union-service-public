package com.rarible.protocol.union.enrichment.model.legacy

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import java.math.BigInteger

@Deprecated("Should be replaced by implementation without token/tokenId")
data class LegacyShortItemId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger
) {

    constructor(dto: ShortItemId) : this(
        dto.blockchain,
        // TODO won't work with Solana
        dto.itemId.substringBefore(IdParser.DELIMITER),
        BigInteger(dto.itemId.substringAfter(IdParser.DELIMITER))
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