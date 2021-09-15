package com.rarible.protocol.union.dto.flow.parser

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.flow.FlowContract
import com.rarible.protocol.union.dto.flow.FlowItemIdDto
import java.math.BigInteger

object FlowItemIdParser {

    /**
     * For full qualifiers like "FLOW:abc:123"
     */
    fun parseFull(value: String): FlowItemIdDto {
        val parts = IdParser.split(value, 3)
        val blockchain = FlowBlockchainDto.valueOf(parts[0])
        return FlowItemIdDto(
            blockchain = blockchain,
            token = FlowContract(blockchain, parts[1]),
            tokenId = BigInteger(parts[2])
        )
    }

    /**
     * For short qualifiers like "abc:123", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: FlowBlockchainDto): FlowItemIdDto {
        val parts = IdParser.split(value, 2)
        return FlowItemIdDto(
            blockchain = blockchain,
            token = FlowContract(blockchain, parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }

}
