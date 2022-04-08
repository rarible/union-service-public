package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionOwnershipEventHandler(
    private val eventProducer: UnionInternalBlockchainEventProducer
) : IncomingEventHandler<UnionOwnershipEvent> {

    // Ownership events should be sent to internal topic to avoid concurrent updates in Enrichment
    override suspend fun onEvent(event: UnionOwnershipEvent) {
        eventProducer.getProducer(event.ownershipId.blockchain)
            .send(KafkaEventFactory.internalOwnershipEvent(event))
    }
}
