package com.rarible.protocol.union.worker.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class MetaRefreshMetrics(meterRegistry: MeterRegistry) {
    private val queueSize = AtomicLong(0)
    private val runningSize = AtomicInteger(0)

    init {
        meterRegistry.gauge("meta_refresh_queue_size", queueSize)
        meterRegistry.gauge("meta_refresh_running_size", runningSize)
    }

    fun queueSize(size: Long) {
        queueSize.set(size)
    }

    fun runningSize(size: Int) {
        runningSize.set(size)
    }
}
