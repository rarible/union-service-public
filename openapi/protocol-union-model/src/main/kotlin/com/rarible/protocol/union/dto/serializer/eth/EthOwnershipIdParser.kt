package com.rarible.protocol.union.dto.serializer.eth

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.EthOwnershipIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object EthOwnershipIdParser {

    /**
     * For full qualifiers like "ETHEREUM:abc:123:xyz"
     */
    fun parseFull(value: String): EthOwnershipIdDto {
        val parts = IdParser.split(value, 4, BlockchainDto.ETHEREUM)
        return EthOwnershipIdDto(
            value = "${parts[1]}:${parts[2]}:${parts[3]}",
            token = EthAddress(parts[1]),
            tokenId = BigInteger(parts[2]),
            owner = EthAddress(parts[3])
        )
    }

    /**
     * For short qualifiers like "abc:123:xyz"
     */
    fun parseShort(value: String): EthOwnershipIdDto {
        val parts = IdParser.split(value, 3)
        return EthOwnershipIdDto(
            value = value,
            token = EthAddress(parts[0]),
            tokenId = BigInteger(parts[1]),
            owner = EthAddress(parts[2])
        )
    }
}
