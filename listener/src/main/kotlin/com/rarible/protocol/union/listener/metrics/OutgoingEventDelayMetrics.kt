package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.UnionMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.BlockchainDto
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
        record(
            PROTOCOL_EVENT_STAGE_DELAY,
            delay,
            tag(blockchain),
            type(type.value),
            source(source),
            tag("from", from),
            tag("to", to)
        )
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
        record(
            PROTOCOL_EVENT_GLOBAL_DELAY,
            delay,
            PROTOCOL_EVENT_GLOBAL_DELAY_OBJECTIVES,
            tag(blockchain),
            type(type.value),
            source(source),
            tag("from", from),
            tag("to", to)
        )
    }

    private companion object {

        const val PROTOCOL_EVENT_STAGE_DELAY = "protocol_event_stage_delay"
        const val PROTOCOL_EVENT_GLOBAL_DELAY = "protocol_event_global_delay"
        val PROTOCOL_EVENT_GLOBAL_DELAY_OBJECTIVES = listOf(
            Duration.ofSeconds(1),
            Duration.ofSeconds(3),
            Duration.ofSeconds(5),
            Duration.ofSeconds(12),
            Duration.ofSeconds(15),
            Duration.ofSeconds(20),
            Duration.ofSeconds(30),
            Duration.ofMinutes(1),
            Duration.ofMinutes(2)
        )
    }
}
