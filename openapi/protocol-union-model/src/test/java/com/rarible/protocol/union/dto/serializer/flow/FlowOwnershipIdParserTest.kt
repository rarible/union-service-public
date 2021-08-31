package com.rarible.protocol.union.dto.serializer.flow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowOwnershipIdParserTest {

    @Test
    fun `parse full`() {
        val value = "FLOW:abc:123:xyz"
        val ownershipId = FlowOwnershipIdParser.parseFull(value)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123:xyz"
        val ownershipId = FlowOwnershipIdParser.parseShort(value)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

}