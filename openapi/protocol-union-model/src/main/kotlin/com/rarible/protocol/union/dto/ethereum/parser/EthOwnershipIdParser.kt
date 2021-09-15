package com.rarible.protocol.union.dto.ethereum.parser

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.ethereum.EthAddress
import com.rarible.protocol.union.dto.ethereum.EthOwnershipIdDto
import java.math.BigInteger

object EthOwnershipIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123:xyz"
     */
    fun parseFull(value: String): EthOwnershipIdDto {
        val parts = IdParser.split(value, 4)
        val blockchain = EthBlockchainDto.valueOf(parts[0])
        return EthOwnershipIdDto(
            blockchain = blockchain,
            token = EthAddress(blockchain, parts[1]),
            tokenId = BigInteger(parts[2]),
            owner = EthAddress(blockchain, parts[3])
        )
    }

    /**
     * For short qualifiers like "abc:123:xyz", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: EthBlockchainDto): EthOwnershipIdDto {
        val parts = IdParser.split(value, 3)
        return EthOwnershipIdDto(
            blockchain = blockchain,
            token = EthAddress(blockchain, parts[0]),
            tokenId = BigInteger(parts[1]),
            owner = EthAddress(blockchain, parts[2])
        )
    }
}
