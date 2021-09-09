package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthOrderIdDto
import com.rarible.protocol.union.dto.IdParser

object EthOrderIdProvider {

    /**
     * For full qualifiers like "ETHEREUM:abc"
     */
    fun parseFull(value: String): EthOrderIdDto {
        val parts = IdParser.split(value, 2)
        val blockchain = EthBlockchainDto.valueOf(parts[0])
        return EthOrderIdDto(
            value = parts[1],
            blockchain = blockchain
        )
    }

    fun create(hash: String, blockchain: EthBlockchainDto): EthOrderIdDto {
        return EthOrderIdDto(
            blockchain = blockchain,
            value = hash
        )
    }

}
