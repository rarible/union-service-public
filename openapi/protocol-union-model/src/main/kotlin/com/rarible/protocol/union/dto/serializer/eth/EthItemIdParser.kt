package com.rarible.protocol.union.dto.serializer.eth

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object EthItemIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123"
     */
    fun parseFull(value: String): EthItemIdDto {
        val parts = IdParser.split(value, 3, BlockchainDto.ETHEREUM)
        return EthItemIdDto(
            value = "${parts[1]}:${parts[2]}",
            token = EthAddress(parts[1]),
            tokenId = BigInteger(parts[2])
        )
    }

    /**
     * For short qualifiers like "abc:123"
     */
    fun parseShort(value: String): EthItemIdDto {
        val parts = IdParser.split(value, 2)
        return EthItemIdDto(
            value = value,
            token = EthAddress(parts[0]),
            tokenId = BigInteger(parts[1])
        )
    }
}
