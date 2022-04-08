package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionInternalOrderEventHandler(
    private val orderEventService: EnrichmentOrderEventService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val reconciliationEventService: ReconciliationEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("UnionOrderEvent")
    suspend fun onEvent(event: UnionOrderEvent) {
        try {
            when (event) {
                is UnionOrderUpdateEvent -> {
                    if (event.order.taker == null) {
                        // Since there could be delay of message delivery, it's better to re-fetch order
                        // to have it in actual state
                        val order = enrichmentOrderService.getById(event.order.id)
                            ?: throw UnionNotFoundException("Order [{}] not found in blockchain")

                        orderEventService.updateOrder(order, true)
                    } else {
                        logger.info("Ignored ${event.order.id} with filled taker")
                    }
                }
            }
        } catch (e: Throwable) {
            reconciliationEventService.onFailedOrder(event.order)
            throw e
        }
    }

}