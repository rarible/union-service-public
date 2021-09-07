package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowOwnershipIdProviderTest {

    @Test
    fun `parse full`() {
        val value = "FLOW:abc:123:xyz"
        val ownershipId = FlowOwnershipIdProvider.parseFull(value)
        assertEquals(FlowBlockchainDto.FLOW, ownershipId.blockchain)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

    @Test
    fun `parse short`() {
        val value = "abc:123:xyz"
        val ownershipId = FlowOwnershipIdProvider.parseShort(value, FlowBlockchainDto.FLOW)
        assertEquals(FlowBlockchainDto.FLOW, ownershipId.blockchain)
        assertEquals("123", ownershipId.tokenId.toString())
        assertEquals("abc", ownershipId.token.value)
        assertEquals("xyz", ownershipId.owner.value)
    }

}