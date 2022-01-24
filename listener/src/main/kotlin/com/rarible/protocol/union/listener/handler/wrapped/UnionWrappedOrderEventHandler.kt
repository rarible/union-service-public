package com.rarible.protocol.union.listener.handler.wrapped

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionWrappedOrderEventHandler(
    private val orderEventService: EnrichmentOrderEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("UnionOrderEvent")
    suspend fun onEvent(event: UnionOrderEvent) {
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