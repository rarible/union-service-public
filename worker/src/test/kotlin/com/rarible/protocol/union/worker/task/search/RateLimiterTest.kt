package com.rarible.protocol.union.worker.task.search

import com.rarible.protocol.union.worker.config.RateLimiterProperties
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class RateLimiterTest {

    private val rateLimiter = EsRateLimiter(RateLimiterProperties(100, 10))

    @Test
    fun `should wait when limit is reached`() = runBlocking<Unit> {
        // given
        val jobs = MutableList(20) { async(start = CoroutineStart.LAZY) { rateLimiter.waitIfNecessary(5) } }

        // when
        val timeSpent = measureTimeMillis {
            jobs.awaitAll()
        }

        // then
        assertThat(timeSpent).isCloseTo(1000, Offset.offset(100))
    }
}
