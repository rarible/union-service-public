package com.rarible.protocol.union.dto.serializer

import com.rarible.protocol.union.dto.BlockchainDto

object IdParser {

    private const val DELIMITER = ":"

    fun parse(value: String, expectedBlockchain: BlockchainDto): Pair<BlockchainDto, String> {
        val index = value.indexOf(DELIMITER)
        if (index < 0) {
            throw IllegalArgumentException("Blockchain name not specified in ID: $value")
        }
        val blockchain = value.substring(0, index)
        val id = value.substring(index + 1)

        assertBlockchain(value, blockchain, expectedBlockchain)
        return Pair(BlockchainDto.valueOf(blockchain), id)
    }

    fun split(value: String, expectedSize: Int, expectedBlockchain: BlockchainDto? = null): List<String> {
        val parts = value.split(DELIMITER)
        assertSize(value, parts, expectedSize, expectedBlockchain)
        assertBlockchain(value, parts[0], expectedBlockchain)
        return parts
    }

    private fun assertBlockchain(id: String, blockchain: String, expectedBlockchain: BlockchainDto? = null) {
        if (expectedBlockchain != null && blockchain.toUpperCase() != expectedBlockchain.name) {
            throw IllegalArgumentException(
                "Illegal blockchain specified in ID: '$id', $expectedBlockchain expected"
            )
        }
    }

    private fun assertSize(
        value: String,
        parts: List<String>,
        expectedSize: Int,
        expectedBlockchain: BlockchainDto? = null
    ) {
        if (parts.size != expectedSize) {
            throw IllegalArgumentException(
                "Illegal format for $expectedBlockchain ID: '$value', " +
                        "expected $expectedSize parts in ID, concatenated by '${DELIMITER}'"
            )
        }
    }

}