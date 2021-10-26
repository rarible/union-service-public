package com.rarible.protocol.union.dto.parser

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger

object ItemIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123"
     */
    fun parseFull(value: String): ItemIdDto {
        val parts = IdParser.split(value, 3)
        val blockchain = IdParser.parseBlockchain(parts[0])
        return ItemIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, parts[1]),
            tokenId = BigInteger(parts[2])
        )
    }

    /**
     * For short qualifiers like "abc:123", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: BlockchainDto): ItemIdDto {
        val parts = IdParser.split(value, 2)
        return ItemIdDto(
            blockchain = blockchain,
            token = UnionAddress(blockchain, parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }
}
