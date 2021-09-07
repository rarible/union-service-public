package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowItemIdDto
import com.rarible.protocol.union.dto.IdParser
import java.math.BigInteger

object FlowItemIdProvider {

    /**
     * For full qualifiers like "FLOW:abc:123"
     */
    fun parseFull(value: String): FlowItemIdDto {
        val parts = IdParser.split(value, 3)
        val blockchain = FlowBlockchainDto.valueOf(parts[0])
        return FlowItemIdDto(
            value = "${parts[1]}:${parts[2]}",
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
            value = value,
            blockchain = blockchain,
            token = FlowContract(blockchain, parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }

    fun create(collection: FlowContract, tokenId: BigInteger, blockchain: FlowBlockchainDto): FlowItemIdDto {
        return FlowItemIdDto(
            value = "${collection.value}:${tokenId}",
            blockchain = blockchain,
            token = collection,
            tokenId = tokenId
        )
    }
}
