package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.listener.handler.internal.IncomingBlockchainEventHandler
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionAuctionEventHandler(
    eventProducer: UnionInternalBlockchainEventProducer
) : IncomingBlockchainEventHandler<UnionAuctionEvent>(eventProducer) {

    override fun toMessage(event: UnionAuctionEvent) = KafkaEventFactory.internalAuctionEvent(event)
    override fun getBlockchain(event: UnionAuctionEvent) = event.auction.id.blockchain
}
