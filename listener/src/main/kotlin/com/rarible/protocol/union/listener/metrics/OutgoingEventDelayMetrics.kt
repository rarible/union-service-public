package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OutgoingEventDelayMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    /**
     * Stage delay means duration between two adjacent marks
     */
    fun markStageDelay(
        blockchain: BlockchainDto,
        type: EventType,
        source: String,
        from: String,
        to: String,
        delay: Duration
    ) {
        markDelay(PROTOCOL_EVENT_STAGE_DELAY, blockchain, type, source, from, to, delay)
    }

    /**
     * Global delay means duration between initial event, or it's trigger, and the time when event left enrichment
     */
    fun markGlobalDelay(
        blockchain: BlockchainDto,
        type: EventType,
        source: String,
        from: String,
        to: String,
        delay: Duration
    ) {
        markDelay(PROTOCOL_EVENT_GLOBAL_DELAY, blockchain, type, source, from, to, delay)
    }

    private fun markDelay(
        name: String,
        blockchain: BlockchainDto,
        type: EventType,
        source: String,
        from: String,
        to: String,
        delay: Duration
    ) {
        meterRegistry.timer(
            name,
            listOf(
                tag(blockchain),
                type(type.value),
                tag("source", source),
                tag("from", from),
                tag("to", to),
            )
        ).record(delay)
    }

    private companion object {

        const val PROTOCOL_EVENT_STAGE_DELAY = "protocol_event_stage_delay"
        const val PROTOCOL_EVENT_GLOBAL_DELAY = "protocol_event_global_delay"
    }
}
