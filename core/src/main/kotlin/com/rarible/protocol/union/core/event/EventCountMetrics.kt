package com.rarible.protocol.union.core.event

import com.rarible.protocol.union.core.UnionMetrics
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class EventCountMetrics(meterRegistry: MeterRegistry) : UnionMetrics(meterRegistry) {
    fun eventReceivedCounter(stage: Stage, blockchain: BlockchainDto, eventType: EventType) = counter(
        EVENT_RECEIVED_METRIC, stage, blockchain, eventType
    )

    fun eventReceived(stage: Stage, blockchain: BlockchainDto, eventType: EventType, count: Int = 1) {
        eventReceivedCounter(stage, blockchain, eventType).increment(count.toDouble())
    }

    fun eventSentCounter(stage: Stage, blockchain: BlockchainDto, eventType: EventType) = counter(
        EVENT_SENT_METRIC, stage, blockchain, eventType
    )

    fun eventSent(stage: Stage, blockchain: BlockchainDto, eventType: EventType, count: Int = 1) {
        eventSentCounter(stage, blockchain, eventType).increment(count.toDouble())
    }

    private fun counter(
        metricName: String,
        stage: Stage,
        blockchain: BlockchainDto,
        eventType: EventType
    ): Counter = meterRegistry.counter(
        metricName, listOf(
            tag(blockchain),
            tag("stage", stage.name.lowercase()),
            type(eventType.name.lowercase()),
        )
    )

    enum class Stage {
        INDEXER, INTERNAL, EXTERNAL
    }

    companion object {
        const val EVENT_RECEIVED_METRIC = "event_received"
        const val EVENT_SENT_METRIC = "event_sent"
    }
}
