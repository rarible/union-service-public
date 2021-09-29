package com.rarible.protocol.union.dto

import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OwnershipIdParserTest {

    @Test
    fun `parse full`() {
        val value = "POLYGON:abc:123:xyz"
        val ownershipId = OwnershipIdParser.parseFull(value)
        assertEquals(BlockchainDto.POLYGON, ownershipId.blockchain)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123:xyz"
        val ownershipId = OwnershipIdParser.parseShort(value, BlockchainDto.ETHEREUM)
        assertEquals(BlockchainDto.ETHEREUM, ownershipId.blockchain)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

}