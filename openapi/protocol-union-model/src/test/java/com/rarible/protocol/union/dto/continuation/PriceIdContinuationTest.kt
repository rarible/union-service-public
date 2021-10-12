package com.rarible.protocol.union.dto.continuation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PriceIdContinuationTest {

    @Test
    fun `parse continuation`() {
        val continuation = PriceIdContinuation.parse("12.3_abc")!!

        assertEquals(12.3.toBigDecimal(), continuation.price)
        assertEquals("abc", continuation.id)
    }

    @Test
    fun `parse continuation - with second underscore`() {
        val continuation = PriceIdContinuation.parse("1.23_abc_ef")!!

        assertEquals(1.23.toBigDecimal(), continuation.price)
        assertEquals("abc_ef", continuation.id)
    }

    @Test
    fun `parse continuation - incorrect format`() {
        assertNull(PriceIdContinuation.parse(null))
        assertNull(PriceIdContinuation.parse(""))
        assertNull(PriceIdContinuation.parse("abc"))
        assertThrows(IllegalArgumentException::class.java) {
            PriceIdContinuation.parse("abc_abc")
        }
    }

    @Test
    fun `compare records`() {
        val p1 = PriceIdContinuation(3.toBigDecimal(), "a")
        val p2 = PriceIdContinuation(2.toBigDecimal(), "z")
        val p3 = PriceIdContinuation(2.toBigDecimal(), "y")
        val p4 = PriceIdContinuation(1.toBigDecimal(), "y")
        val p5 = PriceIdContinuation(1.toBigDecimal(), "x")

        val list = listOf(p1, p2, p3, p4, p5).shuffled().sorted()

        assertEquals(p1, list[0])
        assertEquals(p2, list[1])
        assertEquals(p3, list[2])
        assertEquals(p4, list[3])
        assertEquals(p5, list[4])
    }
}