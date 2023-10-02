package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.producer.UnionInternalAuctionEventProducer
import org.springframework.stereotype.Component

@Component
class UnionAuctionEventHandler(
    private val producer: UnionInternalAuctionEventProducer
) : IncomingEventHandler<UnionAuctionEvent> {

    override suspend fun onEvent(event: UnionAuctionEvent) = producer.send(event)
    override suspend fun onEvents(events: Collection<UnionAuctionEvent>) = producer.send(events)
}
