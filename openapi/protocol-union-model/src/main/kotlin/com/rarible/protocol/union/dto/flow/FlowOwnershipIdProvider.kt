package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowOwnershipIdDto
import com.rarible.protocol.union.dto.IdParser
import java.math.BigInteger

object FlowOwnershipIdProvider {

    /**
     * For full qualifiers like "FLOW:abc:123:xyz"
     */
    fun parseFull(value: String): FlowOwnershipIdDto {
        val parts = IdParser.split(value, 4)
        val blockchain = FlowBlockchainDto.valueOf(parts[0])
        return FlowOwnershipIdDto(
            value = "${parts[1]}:${parts[2]}:${parts[3]}",
            blockchain = blockchain,
            token = FlowContract(blockchain, parts[1]),
            tokenId = BigInteger(parts[2]),
            owner = FlowAddress(blockchain, parts[3])
        )
    }

    /**
     * For short qualifiers like "abc:123:xyz", blockchain should be defined manually
     */
    fun parseShort(value: String, blockchain: FlowBlockchainDto): FlowOwnershipIdDto {
        val parts = IdParser.split(value, 3)
        return FlowOwnershipIdDto(
            value = value,
            blockchain = blockchain,
            token = FlowContract(blockchain, parts[0]),
            tokenId = BigInteger(parts[1]),
            owner = FlowAddress(blockchain, parts[2])
        )
    }

    fun create(
        collection: FlowContract,
        tokenId: BigInteger,
        owner: FlowAddress,
        blockchain: FlowBlockchainDto
    ): FlowOwnershipIdDto {
        return FlowOwnershipIdDto(
            value = "${collection.value}:${tokenId}:${owner.value}",
            blockchain = blockchain,
            token = collection,
            tokenId = tokenId,
            owner = owner
        )
    }
}
