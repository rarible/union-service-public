package com.rarible.protocol.union.core.restriction

import com.rarible.protocol.union.core.model.RestrictionCheckResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RestrictionCheckResultTest {

    @Test
    fun `reduce success`() {
        val success1 = RestrictionCheckResult(true, null)
        val success2 = RestrictionCheckResult(true, "a")

        val firstToSecond = success1.reduce(success2)
        val secondToFirst = success2.reduce(success1)

        val expected = RestrictionCheckResult(true, "a")

        assertThat(firstToSecond).isEqualTo(expected)
        assertThat(secondToFirst).isEqualTo(expected)
    }

    @Test
    fun `reduce errors`() {
        val error1 = RestrictionCheckResult(false, "a")
        val error2 = RestrictionCheckResult(false, "b")

        val firstToSecond = error1.reduce(error2)
        val secondToFirst = error2.reduce(error1)

        val expected1 = RestrictionCheckResult(false, "a; b")
        val expected2 = RestrictionCheckResult(false, "b; a")

        assertThat(firstToSecond).isEqualTo(expected1)
        assertThat(secondToFirst).isEqualTo(expected2)
    }

    @Test
    fun `reduce mixed`() {
        val success = RestrictionCheckResult(true, null)
        val error = RestrictionCheckResult(false, "a")

        val firstToSecond = success.reduce(error)
        val secondToFirst = error.reduce(success)

        val expected = RestrictionCheckResult(false, "a")

        assertThat(firstToSecond).isEqualTo(expected)
        assertThat(secondToFirst).isEqualTo(expected)
    }

}