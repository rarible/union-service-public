package com.rarible.protocol.union.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class IdParserTest {

    @Test
    fun `parse id - ok`() {
        val id = "ETHEREUM:abc"
        val pair = IdParser.parse(id)

        assertEquals(BlockchainDto.ETHEREUM, pair.first)
        assertEquals("abc", pair.second)
    }

    @Test
    fun `parse id - no blockchain`() {
        val id = "abc"
        assertThrows(BlockchainIdFormatException::class.java) {
            IdParser.parse(id)
        }
    }

    @Test
    fun `parse composite id - ok`() {
        val id = "ETHEREUM:abc:123"
        val pair = IdParser.parse(id)

        assertEquals(BlockchainDto.ETHEREUM, pair.first)
        assertEquals("abc:123", pair.second)
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