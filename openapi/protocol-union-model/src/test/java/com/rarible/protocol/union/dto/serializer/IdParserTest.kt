package com.rarible.protocol.union.dto.serializer

import com.rarible.protocol.union.dto.BlockchainDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class IdParserTest {

    @Test
    fun `parse id - ok`() {
        val id = "ETHEREUM:abc"
        val pair = IdParser.parse(id, BlockchainDto.ETHEREUM)

        assertEquals(BlockchainDto.ETHEREUM, pair.first)
        assertEquals("abc", pair.second)
    }

    @Test
    fun `parse id - no blockchain`() {
        val id = "abc"
        assertThrows(IllegalArgumentException::class.java) {
            IdParser.parse(id, BlockchainDto.FLOW)
        }
    }

    @Test
    fun `parse composite id - ok`() {
        val id = "ETHEREUM:abc:123"
        val pair = IdParser.parse(id, BlockchainDto.ETHEREUM)

        assertEquals(BlockchainDto.ETHEREUM, pair.first)
        assertEquals("abc:123", pair.second)
    }

    @Test
    fun `parse composite id - wrong blockchain`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(IllegalArgumentException::class.java) {
            IdParser.parse(id, BlockchainDto.FLOW)
        }
    }

    @Test
    fun `parse composite id - no blockchain`() {
        val id = "abc:123"
        assertThrows(IllegalArgumentException::class.java) {
            IdParser.parse(id, BlockchainDto.FLOW)
        }
    }

    @Test
    fun `split with blockchain - ok`() {
        val id = "ETHEREUM:abc:123"
        val parts = IdParser.split(id, 3, BlockchainDto.ETHEREUM)

        assertEquals("ETHEREUM", parts[0])
        assertEquals("abc", parts[1])
        assertEquals("123", parts[2])
    }

    @Test
    fun `split without blockchain - ok`() {
        val id = "abc:123"
        val parts = IdParser.split(id, 2)

        assertEquals("abc", parts[0])
        assertEquals("123", parts[1])
    }

    @Test
    fun `split with blockchain - wrong blockchain`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(IllegalArgumentException::class.java) {
            IdParser.split(id, 3, BlockchainDto.FLOW)
        }
    }

    @Test
    fun `split with blockchain - wrong size`() {
        val id = "ETHEREUM:abc:123"
        assertThrows(IllegalArgumentException::class.java) {
            IdParser.split(id, 4, BlockchainDto.ETHEREUM)
        }
    }

}