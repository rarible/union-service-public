package com.rarible.protocol.union.dto.serializer.flow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowItemIdParserTest {

    @Test
    fun `parse full`() {
        val value = "FLOW:abc:123"
        val itemId = FlowItemIdParser.parseFull(value)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123"
        val itemId = FlowItemIdParser.parseShort(value)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }
}