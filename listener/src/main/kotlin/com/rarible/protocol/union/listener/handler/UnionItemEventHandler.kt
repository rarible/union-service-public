package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import org.springframework.stereotype.Component

@Component
class UnionItemEventHandler(
    private val producer: UnionInternalItemEventProducer
) : IncomingEventHandler<UnionItemEvent> {

    override suspend fun onEvent(event: UnionItemEvent) = producer.send(addIn(event))
    override suspend fun onEvents(events: Collection<UnionItemEvent>) = producer.send(events.map(::addIn))
    private fun addIn(event: UnionItemEvent) = event.addTimeMark("enrichment-in")
}
