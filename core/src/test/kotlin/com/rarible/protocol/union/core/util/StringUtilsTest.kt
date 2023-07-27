package com.rarible.protocol.union.core.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringUtilsTest {

    @Test
    fun `trim to length`() {
        assertThat(trimToLength("abc", 2, "...")).isEqualTo("ab...")
        assertThat(trimToLength("abc", 2, null)).isEqualTo("ab")
        assertThat(trimToLength("abc", 4, "...")).isEqualTo("abc")
        assertThat(trimToLength("abc", 4, null)).isEqualTo("abc")
        assertThat(trimToLength(null, 1, null)).isEqualTo(null)
    }

    @Test
    fun `safe split`() {
        assertThat(safeSplit("a,b,c")).isEqualTo(listOf("a", "b", "c"))
        assertThat(safeSplit("a")).isEqualTo(listOf("a"))
        assertThat(safeSplit("a, , ,,")).isEqualTo(listOf("a"))
        assertThat(safeSplit("")).isEqualTo(emptyList<String>())
        assertThat(safeSplit(null)).isEqualTo(emptyList<String>())
    }
}
