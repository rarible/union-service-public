package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Clock
import java.time.Duration
import java.time.Instant

abstract class EntityDelayMeterOutgoingItemEventListener<T>(
    private val clock: Clock,
    private val timer: CompositeRegisteredTimer,
) : OutgoingEventListener<T> {

    override suspend fun onEvent(event: T) {
        val blockchain = extractBlockchain(event)
        val eventTimestamp = extractLastUpdateAt(event)

        if (eventTimestamp != null) {
            val now = clock.instant()
            val eventDelay = now.toEpochMilli() - eventTimestamp.toEpochMilli()

            timer.record(Duration.ofMillis(eventDelay), blockchain)
        }
    }

    protected abstract fun extractLastUpdateAt(event: T): Instant?

    protected abstract fun extractBlockchain(event: T): BlockchainDto
}

