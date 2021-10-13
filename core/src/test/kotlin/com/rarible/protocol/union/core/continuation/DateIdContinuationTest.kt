package com.rarible.protocol.union.core.continuation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class DateIdContinuationTest {

    @Test
    fun `parse continuation`() {
        val continuation = DateIdContinuation.parse("123_abc")!!

        assertEquals(123L, continuation.date.toEpochMilli())
        assertEquals("abc", continuation.id)
    }

    @Test
    fun `parse continuation - with second underscore`() {
        val continuation = DateIdContinuation.parse("123_abc_ef")!!

        assertEquals(123L, continuation.date.toEpochMilli())
        assertEquals("abc_ef", continuation.id)
    }

    @Test
    fun `parse continuation - incorrect format`() {
        assertNull(DateIdContinuation.parse(null))
        assertNull(DateIdContinuation.parse(""))
        assertNull(DateIdContinuation.parse("abc"))
        assertThrows(IllegalArgumentException::class.java) {
            DateIdContinuation.parse("abc_abc")
        }
    }

    @Test
    fun `compare records`() {
        val now = Instant.now()
        val d1 = DateIdContinuation(now.plusMillis(1), "a")
        val d2 = DateIdContinuation(now, "y")
        val d3 = DateIdContinuation(now, "z")
        val d4 = DateIdContinuation(now.minusMillis(1), "x")
        val d5 = DateIdContinuation(now.minusMillis(1), "y")

        val list = listOf(d1, d2, d3, d4, d5).shuffled().sorted()

        assertEquals(d1, list[0])
        assertEquals(d2, list[1])
        assertEquals(d3, list[2])
        assertEquals(d4, list[3])
        assertEquals(d5, list[4])
    }
}