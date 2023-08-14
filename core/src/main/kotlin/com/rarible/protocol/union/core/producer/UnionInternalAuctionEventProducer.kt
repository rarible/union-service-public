package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import org.springframework.stereotype.Component

@Component
class UnionInternalAuctionEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer,
    eventCountMetrics: EventCountMetrics
) : UnionInternalEventProducer<UnionAuctionEvent>(eventProducer, eventCountMetrics) {

    override fun getBlockchain(event: UnionAuctionEvent) = event.auction.id.blockchain
    override fun toMessage(event: UnionAuctionEvent) = KafkaEventFactory.internalAuctionEvent(event)
    override fun getEventType(event: UnionAuctionEvent): EventType = EventType.AUCTION
}
