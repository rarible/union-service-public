package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger

data class ShortItemId(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger
) {

    override fun toString(): String {
        return toDto().value
    }

    fun toDto(): ItemIdDto {
        return ItemIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, token),
            tokenId = tokenId
        )
    }
}