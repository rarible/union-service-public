package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowOrderIdDto
import com.rarible.protocol.union.dto.IdParser

object FlowOrderIdProvider {

    /**
     * For full qualifiers like "FLOW:abc"
     */
    fun parseFull(value: String): FlowOrderIdDto {
        val parts = IdParser.split(value, 2)
        val blockchain = FlowBlockchainDto.valueOf(parts[0])
        return FlowOrderIdDto(
            value = parts[1],
            blockchain = blockchain
        )
    }

    fun create(hash: String, blockchain: FlowBlockchainDto): FlowOrderIdDto {
        return FlowOrderIdDto(
            blockchain = blockchain,
            value = hash
        )
    }

}
