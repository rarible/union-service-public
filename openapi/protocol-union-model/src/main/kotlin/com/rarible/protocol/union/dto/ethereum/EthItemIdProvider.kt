package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.IdParser
import java.math.BigInteger

object EthItemIdProvider {

    /**
     * For full qualifiers like "ETHEREUM:abc:123"
     */
    fun parseFull(value: String): EthItemIdDto {
        val parts = IdParser.split(value, 3)
        val blockchain = EthBlockchainDto.valueOf(parts[0])
        return EthItemIdDto(
            value = "${parts[1]}:${parts[2]}",
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
            value = value,
            blockchain = blockchain,
            token = EthAddress(blockchain, parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }

    fun create(token: EthAddress, tokenId: BigInteger, blockchain: EthBlockchainDto): EthItemIdDto {
        return EthItemIdDto(
            blockchain = blockchain,
            value = "${token.value}:${tokenId}",
            token = token,
            tokenId = tokenId
        )
    }
}
