package com.rarible.protocol.union.dto.serializer.flow

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAddress
import com.rarible.protocol.union.dto.FlowContract
import com.rarible.protocol.union.dto.FlowOwnershipIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object FlowOwnershipIdParser {

    /**
     * For full qualifiers like "FLOW:abc:123:xyz"
     */
    fun parseFull(value: String): FlowOwnershipIdDto {
        val parts = IdParser.split(value, 4, BlockchainDto.FLOW)
        return FlowOwnershipIdDto(
            value = "${parts[1]}:${parts[2]}:${parts[3]}",
            token = FlowContract(parts[1]),
            tokenId = BigInteger(parts[2]),
            owner = FlowAddress(parts[3])
        )
    }

    /**
     * For short qualifiers like "abc:123:xyz"
     */
    fun parseShort(value: String): FlowOwnershipIdDto {
        val parts = IdParser.split(value, 3)
        return FlowOwnershipIdDto(
            value = value,
            token = FlowContract(parts[0]),
            tokenId = BigInteger(parts[1]),
            owner = FlowAddress(parts[2])
        )
    }
}
