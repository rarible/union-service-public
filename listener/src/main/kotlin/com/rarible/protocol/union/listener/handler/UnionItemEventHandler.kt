package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.listener.handler.internal.IncomingBlockchainEventHandler
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionItemEventHandler(
    eventProducer: UnionInternalBlockchainEventProducer
) : IncomingBlockchainEventHandler<UnionItemEvent>(eventProducer) {

    override fun toMessage(event: UnionItemEvent) = KafkaEventFactory.internalItemEvent(event)
    override fun getBlockchain(event: UnionItemEvent) = event.itemId.blockchain
}
