package com.rarible.protocol.union.dto

import com.rarible.protocol.union.dto.parser.ItemIdParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemIdParserTest {

    @Test
    fun `parse full`() {
        val value = "ETHEREUM:abc:123"
        val itemId = ItemIdParser.parseFull(value)
        assertEquals(BlockchainDto.ETHEREUM, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123"
        val itemId = ItemIdParser.parseShort(value, BlockchainDto.POLYGON)
        assertEquals(BlockchainDto.POLYGON, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

}