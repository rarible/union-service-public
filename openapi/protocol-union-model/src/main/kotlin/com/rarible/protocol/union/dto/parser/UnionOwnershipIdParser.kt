package com.rarible.protocol.union.dto.parser

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionOwnershipIdDto
import java.math.BigInteger

object UnionOwnershipIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123:xyz"
     */
    fun parseFull(value: String): UnionOwnershipIdDto {
        val parts = IdParser.split(value, 4)
        val blockchain = BlockchainDto.valueOf(parts[0])
        return UnionOwnershipIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, parts[1]),
            tokenId = BigInteger(parts[2]),
            owner = UnionAddress(blockchain, parts[3])
        )
    }

    /**
     * For short qualifiers like "abc:123:xyz", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: BlockchainDto): UnionOwnershipIdDto {
        val parts = IdParser.split(value, 3)
        return UnionOwnershipIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, parts[0]),
            tokenId = BigInteger(parts[1]),
            owner = UnionAddress(blockchain, parts[2])
        )
    }
}
