package com.rarible.protocol.union.dto.serializer.eth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EthItemIdParserTest {

    @Test
    fun `parse full`() {
        val value = "ETHEREUM:abc:123"
        val itemId = EthItemIdParser.parseFull(value)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123"
        val itemId = EthItemIdParser.parseShort(value)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

}