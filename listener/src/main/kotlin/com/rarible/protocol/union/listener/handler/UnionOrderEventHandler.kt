package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.producer.UnionInternalOrderEventProducer
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionOrderEventHandler(
    private val producer: UnionInternalOrderEventProducer
) : IncomingEventHandler<UnionOrderEvent> {

    override suspend fun onEvent(event: UnionOrderEvent) = producer.send(addIn(event))
    override suspend fun onEvents(events: Collection<UnionOrderEvent>) = producer.send(events.map(::addIn))
    private fun addIn(event: UnionOrderEvent) = event.addTimeMark("enrichment-in")
}
