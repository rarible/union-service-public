package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionOwnershipEventHandler(
    private val producer: UnionInternalOwnershipEventProducer
) : IncomingEventHandler<UnionOwnershipEvent> {

    override suspend fun onEvent(event: UnionOwnershipEvent) = producer.send(addIn(event))
    override suspend fun onEvents(events: Collection<UnionOwnershipEvent>) = producer.send(events.map(::addIn))
    private fun addIn(event: UnionOwnershipEvent) = event.addTimeMark("enrichment-in")
}
