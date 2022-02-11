package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import java.math.BigInteger

@Deprecated("Should be replaced by implementation without token/tokenId")
data class ShortOwnershipId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String
) {

    constructor(dto: OwnershipIdDto) : this(
        dto.blockchain,
        // TODO won't work with Solana
        dto.itemIdValue.substringBefore(IdParser.DELIMITER),
        BigInteger(dto.itemIdValue.substringAfter(IdParser.DELIMITER)),
        dto.owner.value
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