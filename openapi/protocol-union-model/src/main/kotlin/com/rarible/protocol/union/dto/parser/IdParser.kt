package com.rarible.protocol.union.dto.parser

import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainIdFormatException
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.UnionAddress

object IdParser {

    private const val DELIMITER = ":"

    fun parseBlockchain(value: String): BlockchainDto {
        try {
            return BlockchainDto.valueOf(value)
        } catch (e: Throwable) {
            throw BlockchainIdFormatException("Unsupported blockchain value: $value, supported are:" +
                    " ${BlockchainDto.values().map { it.name }.joinToString { "|" }}"
            )
        }
    }

    fun parseAddress(value: String): UnionAddress {
        val pair = split(value, 2)
        return UnionAddress(parseBlockchain(pair[0]), pair[1])
    }

    fun parseOrderId(value: String): OrderIdDto {
        val pair = split(value, 2)
        return OrderIdDto(parseBlockchain(pair[0]), pair[1])
    }

    fun parseActivityId(value: String): ActivityIdDto {
        val pair = split(value, 2)
        return ActivityIdDto(parseBlockchain(pair[0]), pair[1])
    }

    private fun parse(value: String): Pair<BlockchainDto, String> {
        val index = value.indexOf(DELIMITER)
        if (index < 0) {
            throw BlockchainIdFormatException("Blockchain name not specified in ID: $value")
        }
        val blockchain = value.substring(0, index)
        val id = value.substring(index + 1)

        return Pair(parseBlockchain(blockchain), id)
    }

    fun split(value: String, expectedSize: Int): List<String> {
        val parts = value.split(DELIMITER)
        assertSize(value, parts, expectedSize)
        return parts
    }

    private fun assertSize(
        value: String,
        parts: List<String>,
        expectedSize: Int
    ) {
        if (parts.size != expectedSize) {
            throw BlockchainIdFormatException(
                "Illegal format for ID: '$value', " +
                        "expected $expectedSize parts in ID, concatenated by '$DELIMITER'"
            )
        }
    }

}