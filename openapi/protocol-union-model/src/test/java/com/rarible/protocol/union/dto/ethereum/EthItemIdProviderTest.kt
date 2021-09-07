package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EthItemIdProviderTest {

    @Test
    fun `parse full`() {
        val value = "ETHEREUM:abc:123"
        val itemId = EthItemIdProvider.parseFull(value)
        assertEquals(EthBlockchainDto.ETHEREUM, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123"
        val itemId = EthItemIdProvider.parseShort(value, EthBlockchainDto.POLYGON)
        assertEquals(EthBlockchainDto.POLYGON, itemId.blockchain)
        assertEquals("123", itemId.tokenId.toString())
        assertEquals("abc", itemId.token.value)
    }

}