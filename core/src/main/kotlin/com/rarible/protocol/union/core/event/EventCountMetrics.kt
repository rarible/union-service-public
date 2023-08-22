package com.rarible.protocol.union.core.event

import com.rarible.protocol.union.core.UnionMetrics
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class EventCountMetrics(meterRegistry: MeterRegistry) : UnionMetrics(meterRegistry) {
    private val gaugeMetricCache: MutableMap<GaugeMetricKey, AtomicInteger> = ConcurrentHashMap()

    fun eventReceivedGauge(stage: Stage, blockchain: BlockchainDto, eventType: EventType) =
        gaugeMetricCache.computeIfAbsent(GaugeMetricKey(EVENT_RECEIVED_METRIC, stage, blockchain, eventType)) {
            gauge(EVENT_RECEIVED_METRIC, stage, blockchain, eventType)
        }

    fun eventReceived(stage: Stage, blockchain: BlockchainDto, eventType: EventType, count: Int = 1) {
        eventReceivedGauge(stage, blockchain, eventType).addAndGet(count)
    }

    fun eventSentGauge(stage: Stage, blockchain: BlockchainDto, eventType: EventType) =
        gaugeMetricCache.computeIfAbsent(GaugeMetricKey(EVENT_SENT_METRIC, stage, blockchain, eventType)) {
            gauge(
                EVENT_SENT_METRIC,
                stage,
                blockchain,
                eventType
            )
        }

    fun eventSent(stage: Stage, blockchain: BlockchainDto, eventType: EventType, count: Int = 1) {
        eventSentGauge(stage, blockchain, eventType).addAndGet(count)
    }

    private fun gauge(
        metricName: String,
        stage: Stage,
        blockchain: BlockchainDto,
        eventType: EventType
    ): AtomicInteger {
        val i = AtomicInteger(0)
        meterRegistry.gauge(
            metricName,
            listOf(
                tag(blockchain),
                tag("stage", stage.name.lowercase()),
                type(eventType.name.lowercase()),
            ),
            i,
            AtomicInteger::toDouble
        )
        return i
    }

    enum class Stage {
        INDEXER, INTERNAL, EXTERNAL
    }

    private data class GaugeMetricKey(
        val metricName: String,
        val stage: Stage,
        val blockchain: BlockchainDto,
        val eventType: EventType
    )

    companion object {
        const val EVENT_RECEIVED_METRIC = "event_received"
        const val EVENT_SENT_METRIC = "event_sent"
    }
}
