package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionCollectionEventHandler(
    private val eventProducer: UnionInternalBlockchainEventProducer
) : IncomingEventHandler<UnionCollectionEvent> {

    override suspend fun onEvent(event: UnionCollectionEvent) {
        eventProducer.getProducer(event.collectionId.blockchain)
            .send(KafkaEventFactory.internalCollectionEvent(event))
    }
}

