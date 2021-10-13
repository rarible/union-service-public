package com.rarible.protocol.union.core.continuation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UsdPriceIdContinuationTest {

    @Test
    fun `to string`() {
        val full = UsdPriceIdContinuation("", dec(1), dec(4), "a")
        val priceAndId = UsdPriceIdContinuation("", dec(1), null, "b")
        val emptyDesc = UsdPriceIdContinuation("", null, null, "c")
        val emptyAsc = UsdPriceIdContinuation("", null, null, "d", true)

        assertThat(full.toString()).isEqualTo("1_a")
        assertThat(priceAndId.toString()).isEqualTo("1_b")
        assertThat(emptyDesc.toString()).isEqualTo("9223372036854775807_c")
        assertThat(emptyAsc.toString()).isEqualTo("0_d")
    }

    @Test
    fun `compare records - desc`() {
        // Currency supports USD conversion
        val c = "c"
        // Currencies without USD conversions
        val c1 = "c1"
        val c2 = "c2"

        val sorted = listOf(
            // Greatest USD price - first
            UsdPriceIdContinuation(c, dec(1), dec(4), "a0"),
            // Same price in USD/Currency, y should be before z
            UsdPriceIdContinuation(c, dec(2), dec(3), "y0"),
            UsdPriceIdContinuation(c, dec(2), dec(3), "z0"),

            // Greatest price for c1
            UsdPriceIdContinuation(c1, dec(2), null, "y1"),
            // Same prices for c1, ordered by ID
            UsdPriceIdContinuation(c1, dec(1), null, "y1"),
            UsdPriceIdContinuation(c1, dec(1), null, "z1"),
            // Null should be last in any case, but in c1 sublist
            UsdPriceIdContinuation(c1, null, null, "a1"),

            // Greatest price for c2
            UsdPriceIdContinuation(c2, dec(25), null, "y2"),
            // Same prices for c2, ordered by ID
            UsdPriceIdContinuation(c2, dec(10), null, "y2"),
            UsdPriceIdContinuation(c2, dec(10), null, "z2"),
            // Null should be last in any case, but in c2 sublist
            UsdPriceIdContinuation(c2, null, null, "a2")
        )

        val list = sorted.shuffled().sorted()

        for (i in list.indices) {
            assertThat(list[i]).isEqualTo(sorted[i])
        }
    }

    @Test
    fun `compare records - asc`() {
        // Currency supports USD conversion
        val c = "c"
        // Currencies without USD conversions
        val c1 = "c1"
        val c2 = "c2"

        val sorted = listOf(
            // Greatest USD prices - first, then by id
            UsdPriceIdContinuation(c, dec(2), dec(3), "y0", true),
            UsdPriceIdContinuation(c, dec(2), dec(3), "z0", true),
            UsdPriceIdContinuation(c, dec(1), dec(4), "a0", true),

            // Greatest prices for c1 - first, then by id
            UsdPriceIdContinuation(c1, dec(1), null, "y1", true),
            UsdPriceIdContinuation(c1, dec(1), null, "z1", true),
            UsdPriceIdContinuation(c1, dec(2), null, "y1", true),
            // Null should be last in any case, but in c1 sublist
            UsdPriceIdContinuation(c1, null, null, "a1", true),

            // Greatest prices for c2 - first, then by id
            UsdPriceIdContinuation(c2, dec(10), null, "y2", true),
            UsdPriceIdContinuation(c2, dec(25), null, "y2", true),
            UsdPriceIdContinuation(c2, dec(25), null, "z2", true),
            // Null should be last in any case, but in c2 sublist
            UsdPriceIdContinuation(c2, null, null, "a2", true)
        )

        val list = sorted.shuffled().sorted()

        for (i in list.indices) {
            assertThat(list[i]).isEqualTo(sorted[i])
        }
    }

    private fun dec(v: Int): BigDecimal {
        return v.toBigDecimal()
    }
}