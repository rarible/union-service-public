package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionAmmOrderNftUpdateEvent
import com.rarible.protocol.union.core.model.UnionAmmOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionInternalOrderEventHandler(
    private val orderEventService: EnrichmentOrderEventService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val reconciliationEventService: ReconciliationEventService,
    private val handler: IncomingEventHandler<UnionOrderEvent>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("UnionOrderEvent")
    suspend fun onEvent(event: UnionOrderEvent) {
        try {
            when (event) {
                is UnionOrderUpdateEvent -> onOrderUpdate(event.order)
                is UnionAmmOrderUpdateEvent -> onAmmOrderUpdate(event.order, event.itemId)
                is UnionAmmOrderNftUpdateEvent -> onAmmOrderNftUpdate(event)
            }
        } catch (e: Throwable) {
            // TODO PT-1151 not really sure how to perform reconciliation for AMM orders
            val order = when (event) {
                is UnionOrderUpdateEvent -> event.order
                is UnionAmmOrderUpdateEvent -> event.order
                else -> null
            }
            order?.let { reconciliationEventService.onFailedOrder(order) }
            throw e
        }
    }

    // Regular update event
    private suspend fun onOrderUpdate(order: OrderDto) {
        if (order.platform == PlatformDto.SUDOSWAP) {
            // TODO PT-1151 maybe there can be better way to determine such orders?
            updateAmmOrder(fetchOrder(order.id))
            return
        }

        if (order.taker == null) {
            orderEventService.updateOrder(fetchOrder(order.id), true)
        } else {
            logger.info("Ignored ${order.id} with filled taker")
        }
    }

    // Synthetic update event
    private suspend fun onAmmOrderUpdate(order: OrderDto, itemId: ItemIdDto) {
        orderEventService.updateAmmOrder(fetchOrder(order.id), itemId)
    }

    private suspend fun onAmmOrderNftUpdate(event: UnionAmmOrderNftUpdateEvent) {
        // TODO PT-1151 update ShortItems then trigger synthetic events
        updateAmmOrder(fetchOrder(event.orderId))
    }

    // I think we have to trigger updates for ALL items related to AMM order in any case
    private suspend fun updateAmmOrder(order: OrderDto) {
        val itemIds = emptyList<ItemIdDto>() //TODO PT-1151 find related items
        itemIds.forEach {
            handler.onEvent(UnionAmmOrderUpdateEvent(order, it))
        }
    }

    private suspend fun fetchOrder(orderId: OrderIdDto): OrderDto {
        // Since there could be delay of message delivery, it's better to re-fetch order
        // to have it in actual state
        return enrichmentOrderService.getById(orderId)
            ?: throw UnionNotFoundException("Order [{}] not found in blockchain")
    }

}