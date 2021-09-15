package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.ethereum.parser.EthItemIdParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EthItemIdParserTest {

    @Test
    fun `parse full`() {
        val value = "ETHEREUM:abc:123"
        val itemId = EthItemIdParser.parseFull(value)
        assertEquals(EthBlockchainDto.ETHEREUM, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123"
        val itemId = EthItemIdParser.parseShort(value, EthBlockchainDto.POLYGON)
        assertEquals(EthBlockchainDto.POLYGON, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

}