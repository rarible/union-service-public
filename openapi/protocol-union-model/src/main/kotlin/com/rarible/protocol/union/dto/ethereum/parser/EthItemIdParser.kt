package com.rarible.protocol.union.dto.ethereum.parser

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.ethereum.EthAddress
import com.rarible.protocol.union.dto.ethereum.EthItemIdDto
import java.math.BigInteger

object EthItemIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123"
     */
    fun parseFull(value: String): EthItemIdDto {
        val parts = IdParser.split(value, 3)
        val blockchain = EthBlockchainDto.valueOf(parts[0])
        return EthItemIdDto(
            blockchain = blockchain,
            token = EthAddress(blockchain, parts[1]),
            tokenId = BigInteger(parts[2])
        )
    }

    /**
     * For short qualifiers like "abc:123", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: EthBlockchainDto): EthItemIdDto {
        val parts = IdParser.split(value, 2)
        return EthItemIdDto(
            blockchain = blockchain,
            token = EthAddress(blockchain, parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }
}
