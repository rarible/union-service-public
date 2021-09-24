package com.rarible.protocol.union.dto.parser

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionItemIdDto
import java.math.BigInteger

object UnionItemIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123"
     */
    fun parseFull(value: String): UnionItemIdDto {
        val parts = IdParser.split(value, 3)
        val blockchain = BlockchainDto.valueOf(parts[0])
        return UnionItemIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, parts[1]),
            tokenId = BigInteger(parts[2])
        )
    }

    /**
     * For short qualifiers like "abc:123", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: BlockchainDto): UnionItemIdDto {
        val parts = IdParser.split(value, 2)
        return UnionItemIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }
}
