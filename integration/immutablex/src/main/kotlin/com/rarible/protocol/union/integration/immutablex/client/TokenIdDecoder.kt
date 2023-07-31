package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.parser.IdParser
import java.math.BigInteger
import java.util.Arrays

object TokenIdDecoder {

    private val stringTokenPrefix = "STR_T_ID".toByteArray()

    fun encode(tokenId: String): BigInteger {
        return try {
            tokenId.toBigInteger()
        } catch (e: Exception) {
            // In testnet there are invalid tokenIds in string representation like UUID
            // https://api.ropsten.x.immutable.com/v1/assets/0x6de6b04d630a4a41bb223815433b9ebf0da50f69/8e842633-fe3d-4e30-a93d-e5c0b0c940ac
            // As per IMX support, there is no chance to meet such tokenIds at mainnet, but we have to parse them
            // somehow in order to provide possibility to test at testnet

            val array = stringTokenPrefix.plus(tokenId.toByteArray())
            return BigInteger(array)
        }
    }

    fun decode(tokenId: String): String {
        val bigInt = tokenId.toBigInteger()
        val bytes = bigInt.toByteArray()
        if (bytes.size < stringTokenPrefix.size) {
            return tokenId
        }
        val start = Arrays.copyOf(bytes, stringTokenPrefix.size)
        if (!start.contentEquals(stringTokenPrefix)) {
            return tokenId
        }
        val end = Arrays.copyOfRange(bytes, stringTokenPrefix.size, bytes.size)
        return String(end)
    }

    fun decodeItemId(itemId: String): String {
        val (token, rawTokenId) = IdParser.split(itemId, 2)
        val tokenId = decode(rawTokenId)
        return "$token:$tokenId"
    }

    fun encodeItemId(itemId: String): String {
        val (token, rawTokenId) = IdParser.split(itemId, 2)
        val tokenId = encode(rawTokenId)
        return "$token:$tokenId"
    }
}
