package com.rarible.protocol.union.dto

import com.rarible.protocol.union.dto.parser.IdParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class IdParserTest {

    @Test
    fun `parse activity id`() {
        val id = "ETHEREUM:abc"
        val activityId = IdParser.parseActivityId(id)

        assertEquals(BlockchainDto.ETHEREUM, activityId.blockchain)
        assertEquals("abc", activityId.value)
    }

    @Test
    fun `parse order id`() {
        val id = "TEZOS:231"
        val orderId = IdParser.parseOrderId(id)

        assertEquals(BlockchainDto.TEZOS, orderId.blockchain)
        assertEquals("231", orderId.value)
    }

    @Test
    fun `parse address`() {
        val id = "FLOW:gtt"
        val address = IdParser.parseAddress(id)

        assertEquals(BlockchainDto.FLOW, address.blockchain)
        assertEquals("gtt", address.value)
    }

    @Test
    fun `parse address - no blockchain`() {
        val id = "abc"
        assertThrows(BlockchainIdFormatException::class.java) {
            IdParser.parseAddress(id)
        }
    }

    @Test
    fun `parse address - too many parts`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(BlockchainIdFormatException::class.java) {
            IdParser.parseAddress(id)
        }
    }

    @Test
    fun `parse orderId - too many parts`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(BlockchainIdFormatException::class.java) {
            IdParser.parseOrderId(id)
        }
    }

    @Test
    fun `parse activityId - too many parts`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(BlockchainIdFormatException::class.java) {
            IdParser.parseActivityId(id)
        }
    }

    @Test
    fun `split - ok`() {
        val id = "ETHEREUM:abc:123"
        val parts = IdParser.split(id, 3)

        assertEquals("ETHEREUM", parts[0])
        assertEquals("abc", parts[1])
        assertEquals("123", parts[2])
    }

    @Test
    fun `split with blockchain - wrong size`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(BlockchainIdFormatException::class.java) {
            IdParser.split(id, 4)
        }
    }

}