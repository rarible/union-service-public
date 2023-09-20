package com.rarible.protocol.union.core

import com.google.common.util.concurrent.AtomicDouble
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class UnionMetrics(
    protected val meterRegistry: MeterRegistry
) {

    private val gauges = Meters(::createGauge)
    private val counters = Meters(::createCounter)
    private val timers = Meters(::createTimer)

    protected fun tag(blockchain: BlockchainDto): Tag {
        return tag("blockchain", blockchain.name.lowercase())
    }

    protected fun tag(key: String, value: String): Tag {
        return ImmutableTag(key, value)
    }

    protected fun status(status: String): Tag {
        return tag("status", status)
    }

    protected fun type(type: String): Tag {
        return tag("type", type)
    }

    protected fun reason(reason: String): Tag {
        return tag("reason", reason)
    }

    protected fun source(value: String): Tag {
        return tag("source", value)
    }

    protected fun increment(name: String, vararg tags: Tag) {
        counters.getOrCreate(name, tags.toList()).increment()
    }

    protected fun record(name: String, duration: Duration, vararg tags: Tag) {
        timers.getOrCreate(name, tags.toList()).record(duration)
    }

    protected fun record(name: String, duration: Duration, percentiles: List<Double>, vararg tags: Tag) {
        timers.getOrCreate(name, tags.toList()) { inputName, inputTags ->
            Timer.builder(inputName)
                .tags(inputTags)
                .publishPercentiles(*percentiles.toDoubleArray())
                .publishPercentileHistogram()
                .register(meterRegistry)
        }.record(duration)
    }

    protected fun set(name: String, value: Number, vararg tags: Tag) {
        gauges.getOrCreate(name, tags.toList()).set(value.toDouble())
    }

    private fun createGauge(name: String, tags: List<Tag>): AtomicDouble {
        val gauge = AtomicDouble()
        meterRegistry.gauge(name, tags, gauge) { it.get() }
        return gauge
    }

    private fun createTimer(name: String, tags: List<Tag>): Timer {
        return meterRegistry.timer(name, tags)
    }

    private fun createCounter(name: String, tags: List<Tag>): Counter {
        return meterRegistry.counter(name, tags)
    }

    private class Meters<T>(private val defaultConstructor: (name: String, tags: List<Tag>) -> T) {
        private val meters = ConcurrentHashMap<MeterId, T>()
        fun getOrCreate(name: String, tags: List<Tag>) = getOrCreate(name, tags, defaultConstructor)

        fun getOrCreate(name: String, tags: List<Tag>, constructor: (name: String, tags: List<Tag>) -> T): T {
            return meters.computeIfAbsent(MeterId(name, tags)) {
                constructor(name, tags)
            }
        }
    }

    private data class MeterId(
        val name: String,
        val tags: List<Tag> = emptyList()
    )

    companion object {
        val PERCENTILES_99_95_75 = listOf(0.99, 0.95, 0.75)
    }
}
