package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger


data class ShortOwnershipId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String
) {

    override fun toString(): String {
        return toDto().value
    }

    fun toDto(): OwnershipIdDto {
        return OwnershipIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, token),
            tokenId = tokenId,
            owner = UnionAddress(blockchain, owner)
        )
    }
}