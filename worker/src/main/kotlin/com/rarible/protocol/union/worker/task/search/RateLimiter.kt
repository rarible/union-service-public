package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.worker.config.RateLimiterProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.system.measureTimeMillis

@Service
class RateLimiter(
    private val props: RateLimiterProperties,
) {

    private val logger by Logger()

    private val lock = Mutex()

    private var nextPeriodReset = Instant.EPOCH
    private var remainingEntities = props.maxEntities

    suspend fun waitIfNecessary(amount: Int) {
        val elapsedTime = measureTimeMillis {
            lock.withLock {
                if (remainingEntities < amount) {
                    val timeToWait = nextPeriodReset.toEpochMilli() - nowMillis().toEpochMilli()
                    delay(timeToWait)
                    remainingEntities = props.maxEntities
                    nextPeriodReset = nowMillis().plusMillis(props.period)
                }
                remainingEntities -= amount
                if (nextPeriodReset < nowMillis()) {
                    nextPeriodReset = nowMillis().plusMillis(props.period)
                }
            }
        }
        logger.info("Spent $elapsedTime ms waiting in rate limiter")
    }
}
