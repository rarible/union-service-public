package com.rarible.protocol.union.dto.serializer.eth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EthOwnershipIdParserTest {

    @Test
    fun `parse full`() {
        val value = "ETHEREUM:abc:123:xyz"
        val ownershipId = EthOwnershipIdParser.parseFull(value)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123:xyz"
        val ownershipId = EthOwnershipIdParser.parseShort(value)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

}