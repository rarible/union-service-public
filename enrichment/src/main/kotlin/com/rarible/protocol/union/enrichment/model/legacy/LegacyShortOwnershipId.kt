package com.rarible.protocol.union.enrichment.model.legacy

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import java.math.BigInteger

@Deprecated("Should be replaced by implementation without token/tokenId")
data class LegacyShortOwnershipId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String
) {

    constructor(dto: ShortOwnershipId) : this(
        dto.blockchain,
        // TODO won't work with Solana
        dto.itemId.substringBefore(IdParser.DELIMITER),
        BigInteger(dto.itemId.substringAfter(IdParser.DELIMITER)),
        dto.owner
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): OwnershipIdDto {
        return OwnershipIdDto(
            blockchain = blockchain,
            contract = token,
            tokenId = tokenId,
            owner = UnionAddressConverter.convert(blockchain, owner)
        )
    }
}