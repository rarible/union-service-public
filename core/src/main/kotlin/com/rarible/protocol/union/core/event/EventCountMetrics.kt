package com.rarible.protocol.union.core.event

import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class EventCountMetrics(private val meterRegistry: MeterRegistry) {
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
            Tag.of("stage", stage.name.lowercase()),
            Tag.of("blockchain", blockchain.name.lowercase()),
            Tag.of("eventType", eventType.name.lowercase()),
        )
    )

    enum class Stage {
        INDEXER, INTERNAL, EXTERNAL
    }

    companion object {
        const val EVENT_RECEIVED_METRIC = "protocol.union.event.received"
        const val EVENT_SENT_METRIC = "protocol.union.event.sent"
    }
}
