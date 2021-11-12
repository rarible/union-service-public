package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "service", subtype = "event")
class UnionOrderEventHandler(
    private val orderEventService: EnrichmentOrderEventService
) : IncomingEventHandler<UnionOrderEvent> {

    override suspend fun onEvent(event: UnionOrderEvent) {
        when (event) {
            is UnionOrderUpdateEvent -> {
                orderEventService.updateOrder(event.order, true)
            }
        }
    }
}
