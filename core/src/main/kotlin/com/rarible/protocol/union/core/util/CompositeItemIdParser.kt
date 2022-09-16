package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.dto.parser.IdParser
import java.math.BigInteger

// Simple parser for composite Item ids (ETH, TEZOS, FLOW uses this format with token:tokenId)
object CompositeItemIdParser {

    fun split(itemIdWithoutBlockchain: String): Pair<String, BigInteger> {
        val pair = IdParser.split(itemIdWithoutBlockchain, 2)
        val token = pair[0]
        val tokenId = UnionConverter.convertToBigInteger(pair[1])
        return Pair(token, tokenId)
    }

    fun splitWithBlockchain(itemId: String): Pair<String, BigInteger?> {
        val parts = IdParser.split(itemId, 2..3)
        return if (parts.size == 3) {
            val blockchain = parts[0]
            val token = parts[1]
            val tokenId = UnionConverter.convertToBigInteger(parts[2])
            Pair("$blockchain:$token", tokenId)
        } else {
            parts[0] + ":" + parts[1] to null
        }
    }
}