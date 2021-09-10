package com.rarible.protocol.union.dto

object IdParser {

    private const val DELIMITER = ":"

    fun parse(value: String): Pair<BlockchainDto, String> {
        val index = value.indexOf(DELIMITER)
        if (index < 0) {
            throw IllegalArgumentException("Blockchain name not specified in ID: $value")
        }
        val blockchain = value.substring(0, index)
        val id = value.substring(index + 1)

        return Pair(BlockchainDto.valueOf(blockchain), id)
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
            throw IllegalArgumentException(
                "Illegal format for ID: '$value', " +
                        "expected $expectedSize parts in ID, concatenated by '$DELIMITER'"
            )
        }
    }

}