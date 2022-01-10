package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionOrderEventHandler(
    private val orderEventService: EnrichmentOrderEventService
) : IncomingEventHandler<UnionOrderEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: UnionOrderEvent) {
        when (event) {
            is UnionOrderUpdateEvent -> {
                if (event.order.taker == null) {
                    orderEventService.updateOrder(event.order, true)
                } else {
                    logger.info("Ignored ${event.order.id} with filled taker")
                }
            }
        }
    }
}
