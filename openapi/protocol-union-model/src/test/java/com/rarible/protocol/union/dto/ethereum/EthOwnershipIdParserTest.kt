package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.ethereum.parser.EthOwnershipIdParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EthOwnershipIdParserTest {

    @Test
    fun `parse full`() {
        val value = "POLYGON:abc:123:xyz"
        val ownershipId = EthOwnershipIdParser.parseFull(value)
        assertEquals(EthBlockchainDto.POLYGON, ownershipId.blockchain)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123:xyz"
        val ownershipId = EthOwnershipIdParser.parseShort(value, EthBlockchainDto.ETHEREUM)
        assertEquals(EthBlockchainDto.ETHEREUM, ownershipId.blockchain)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

}