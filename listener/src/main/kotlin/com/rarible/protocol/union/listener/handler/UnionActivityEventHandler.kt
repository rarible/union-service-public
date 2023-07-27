package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.producer.UnionInternalActivityEventProducer
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionActivityEventHandler(
    private val producer: UnionInternalActivityEventProducer
) : IncomingEventHandler<UnionActivity> {

    override suspend fun onEvent(event: UnionActivity) = producer.send(event)
    override suspend fun onEvents(events: Collection<UnionActivity>) = producer.send(events.map(::addIn))
    private fun addIn(event: UnionActivity) = event.addTimeMark("enrichment-in")
}
