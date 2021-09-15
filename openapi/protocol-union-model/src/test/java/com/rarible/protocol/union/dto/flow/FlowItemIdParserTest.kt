package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.flow.parser.FlowItemIdParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowItemIdParserTest {

    @Test
    fun `parse full`() {
        val value = "FLOW:abc:123"
        val itemId = FlowItemIdParser.parseFull(value)
        assertEquals(FlowBlockchainDto.FLOW, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123"
        val itemId = FlowItemIdParser.parseShort(value, FlowBlockchainDto.FLOW)
        assertEquals(FlowBlockchainDto.FLOW, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }
}