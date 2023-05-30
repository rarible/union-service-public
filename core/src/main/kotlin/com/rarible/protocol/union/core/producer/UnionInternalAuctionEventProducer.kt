package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import org.springframework.stereotype.Component

@Component
class UnionInternalAuctionEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer
) : UnionInternalEventProducer<UnionAuctionEvent>(eventProducer) {

    override fun getBlockchain(event: UnionAuctionEvent) = event.auction.id.blockchain
    override fun toMessage(event: UnionAuctionEvent) = KafkaEventFactory.internalAuctionEvent(event)

}
