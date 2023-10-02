package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import org.springframework.stereotype.Component

@Component
class UnionCollectionEventHandler(
    private val producer: UnionInternalCollectionEventProducer
) : IncomingEventHandler<UnionCollectionEvent> {

    override suspend fun onEvent(event: UnionCollectionEvent) = producer.send(addIn(event))
    override suspend fun onEvents(events: Collection<UnionCollectionEvent>) = producer.send(events.map(::addIn))
    private fun addIn(event: UnionCollectionEvent) = event.addTimeMark("enrichment-in")
}
