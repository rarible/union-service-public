package com.rarible.protocol.union.worker.task.search.activity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TimePeriodContinuationHelperTest {

    @Test
    fun `should not adjust when time period is not specified`() {
        // given
        val from = null
        val to = null
        val initial = "ETHEREUM:12345_ABCPDF"

        // when
        val actual = TimePeriodContinuationHelper.adjustContinuation(initial, from, to)

        // then
        assertThat(actual).isEqualTo(initial)
    }

    @Test
    fun `should not adjust when continuation is null`() {
        // given
        val from = 1234L
        val to = 5678L
        val initial = null

        // when
        val actual = TimePeriodContinuationHelper.adjustContinuation(initial, from, to)

        // then
        assertThat(actual).isEqualTo(initial)
    }

    @Test
    fun `should not adjust when continuation is empty`() {
        // given
        val from = 1234L
        val to = 5678L
        val initial = ""

        // when
        val actual = TimePeriodContinuationHelper.adjustContinuation(initial, from, to)

        // then
        assertThat(actual).isEqualTo(initial)
    }

    @Test
    fun `should not adjust when continuation is within range`() {
        // given
        val from = 111L
        val to = 333L
        val initial = "ETHEREUM:222_ABCPDF"

        // when
        val actual = TimePeriodContinuationHelper.adjustContinuation(initial, from, to)

        // then
        assertThat(actual).isEqualTo(initial)
    }

    @Test
    fun `should adjust when continuation is above range`() {
        // given
        val from = 222L
        val to = 444L
        val initial = "ETHEREUM:555_ABCPDF"

        // when
        val actual = TimePeriodContinuationHelper.adjustContinuation(initial, from, to)

        // then
        assertThat(actual).isEqualTo("ETHEREUM:445_ABCPDF")
    }

    @Test
    fun `should adjust when continuation is below range`() {
        // given
        val from = 222L
        val to = 444L
        val initial = "ETHEREUM:111_ABCPDF"

        // when
        val actual = TimePeriodContinuationHelper.adjustContinuation(initial, from, to)

        // then
        assertThat(actual).isNull()
    }
}
