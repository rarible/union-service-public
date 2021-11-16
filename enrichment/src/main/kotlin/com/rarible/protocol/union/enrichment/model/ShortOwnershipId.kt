package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import java.math.BigInteger

data class ShortOwnershipId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String
) {

    constructor(dto: OwnershipIdDto) : this(
        dto.blockchain,
        dto.contract,
        dto.tokenId,
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