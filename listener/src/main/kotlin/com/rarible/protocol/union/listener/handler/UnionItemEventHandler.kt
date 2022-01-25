package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionItemEventHandler(
    private val eventsProducer: RaribleKafkaProducer<UnionWrappedEvent>
) : IncomingEventHandler<UnionItemEvent> {

    // Item events should be sent to internal topic to avoid concurrent updates in Enrichment
    override suspend fun onEvent(event: UnionItemEvent) {
        eventsProducer.send(KafkaEventFactory.wrappedItemEvent(event))
    }
}
