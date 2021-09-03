package com.rarible.protocol.union.dto.serializer.flow

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowContract
import com.rarible.protocol.union.dto.FlowItemIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object FlowItemIdParser {

    /**
     * For full qualifiers like "FLOW:abc:123"
     */
    fun parseFull(value: String): FlowItemIdDto {
        val parts = IdParser.split(value, 3, BlockchainDto.FLOW)
        return FlowItemIdDto(
            value = "${parts[1]}:${parts[2]}",
            token = FlowContract(parts[1]),
            tokenId = BigInteger(parts[2])
        )
    }

    /**
     * For short qualifiers like "abc:123"
     */
    fun parseShort(value: String): FlowItemIdDto {
        val parts = IdParser.split(value, 2)
        return FlowItemIdDto(
            value = value,
            token = FlowContract(parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }
}
