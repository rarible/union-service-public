package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.worker.config.RateLimiterProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.system.measureTimeMillis

@Service
class EsRateLimiter(
    private val props: RateLimiterProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val lock = Mutex()

    @Volatile
    private var nextPeriodReset = Instant.EPOCH

    @Volatile
    private var remainingEntities = 0

    suspend fun waitIfNecessary(amount: Int) {
        val elapsedTime = measureTimeMillis {
            lock.withLock {
                if (remainingEntities < amount) {
                    val timeToWait = nextPeriodReset.toEpochMilli() - nowMillis().toEpochMilli()
                    if (timeToWait > 0) {
                        logger.info("Rate limit exceeded, waiting for $timeToWait ms")
                        delay(timeToWait)
                    }
                    remainingEntities = props.maxEntities
                    nextPeriodReset = Instant.now().plusMillis(props.period)
                }
                remainingEntities -= amount
            }
        }
        // logger.info("Spent $elapsedTime ms waiting in rate limiter")
    }
}
