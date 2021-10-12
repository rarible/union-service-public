package com.rarible.protocol.union.core.continuation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CombinedContinuationTest {

    @Test
    fun `print - empty`() {
        val continuation = CombinedContinuation(emptyMap())

        assertThat(continuation.toString()).isEqualTo("")
        assertThat(CombinedContinuation.parse("")).isEqualTo(continuation)
        assertThat(CombinedContinuation.parse(null)).isEqualTo(continuation)
    }

    @Test
    fun `several fields`() {
        val continuation = CombinedContinuation(
            mapOf(
                "a" to "1",
                "b" to "2:1",
                "c" to "3_4:5"
            )
        )

        val expected = "a:1;b:2:1;c:3_4:5"
        assertThat(continuation.toString()).isEqualTo(expected)

        val parsed = CombinedContinuation.parse(expected)
        assertThat(parsed).isEqualTo(continuation)
    }

    @Test
    fun `single field`() {
        val continuation = CombinedContinuation(
            mapOf(
                "a" to "1"
            )
        )

        val expected = "a:1"
        assertThat(continuation.toString()).isEqualTo(expected)

        val parsed = CombinedContinuation.parse(expected)
        assertThat(parsed).isEqualTo(continuation)
    }
}