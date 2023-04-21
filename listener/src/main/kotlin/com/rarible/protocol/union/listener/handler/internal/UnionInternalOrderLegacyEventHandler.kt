package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderLegacyEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateLegacyEvent
import com.rarible.protocol.union.core.model.UnionPoolNftUpdateLegacyEvent
import com.rarible.protocol.union.core.model.UnionPoolOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionPoolOrderUpdateLegacyEvent
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@Deprecated("keep only UnionInternalOrderEventHandler")
class UnionInternalOrderLegacyEventHandler(
    private val orderEventService: EnrichmentOrderEventService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentItemService: EnrichmentItemService,
    private val reconciliationEventService: ReconciliationEventService,
    private val handler: IncomingEventHandler<UnionOrderEvent>,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("UnionOrderEvent")
    suspend fun onEvent(event: UnionOrderLegacyEvent) {
        try {
            when (event) {
                is UnionOrderUpdateLegacyEvent -> {
                    val unionOrder = fetchOrder(event.order.id)
                    when (unionOrder.isPoolOrder()) {
                        // Trigger events to all existing items placed to the pool
                        true -> triggerPoolOrderUpdate(event.orderId, event.eventTimeMarks)
                        else -> onOrderUpdate(event)
                    }
                }

                is UnionPoolOrderUpdateLegacyEvent -> onPoolOrderUpdate(event)
                // Trigger events to all existing items placed to the pool with including/excluding items
                is UnionPoolNftUpdateLegacyEvent -> triggerPoolOrderUpdate(
                    event.orderId,
                    event.eventTimeMarks,
                    event.inNft,
                    event.outNft
                )
            }
        } catch (e: Throwable) {
            // TODO PT-1151 not really sure how to perform reconciliation for AMM orders
            val order = when (event) {
                is UnionOrderUpdateLegacyEvent -> event.order
                is UnionPoolOrderUpdateLegacyEvent -> event.order
                else -> null
            }
            order?.let {
                val unionOrder = fetchOrder(order.id)
                reconciliationEventService.onFailedOrder(unionOrder)
            }
            throw e
        }
    }

    // Regular update event
    private suspend fun onOrderUpdate(event: UnionOrderUpdateLegacyEvent) {
        val order = event.order
        if (order.taker == null) {
            orderEventService.updateOrder(fetchOrder(order.id), event.eventTimeMarks, true)
        } else {
            logger.info("Ignored ${order.id} with filled taker")
        }
    }

    // Synthetic update event
    private suspend fun onPoolOrderUpdate(event: UnionPoolOrderUpdateLegacyEvent) {
        if (!ff.enablePoolOrders) {
            return
        }
        // for synthetic updates it might be costly to fetch order - so use received one
        val unionOrder = fetchOrder(event.orderId)
        orderEventService.updatePoolOrderPerItem(unionOrder, event.itemId, event.action, event.eventTimeMarks)
    }

    private suspend fun triggerPoolOrderUpdate(
        orderId: OrderIdDto,
        eventTimeMarks: UnionEventTimeMarks?,
        included: Set<ItemIdDto> = emptySet(),
        excluded: Set<ItemIdDto> = emptySet()
    ) {
        if (!ff.enablePoolOrders) {
            return
        }

        val orderDeferred = coroutineScope { async { fetchOrder(orderId) } }
        val exist = enrichmentItemService.findByPoolOrder(orderId)
        val order = orderDeferred.await()

        val messages = ArrayList<UnionPoolOrderUpdateEvent>(exist.size + included.size + excluded.size)

        exist.forEach {
            val itemId = it.toDto()
            if (!excluded.contains(itemId) && !included.contains(itemId)) {
                // For existed short items we should generate event to update best sell orders
                messages.add(UnionPoolOrderUpdateEvent(order, itemId, PoolItemAction.UPDATED, eventTimeMarks))
            }
        }
        included.forEach { messages.add(UnionPoolOrderUpdateEvent(order, it, PoolItemAction.INCLUDED, eventTimeMarks)) }
        excluded.forEach { messages.add(UnionPoolOrderUpdateEvent(order, it, PoolItemAction.EXCLUDED, eventTimeMarks)) }

        // We should not to handle such events here directly since it could cause concurrent modification problems
        // Instead, we send such synthetic events to this internal handler in order to consequently process
        // all events related to each item
        handler.onEvents(messages)
        logger.info(
            "Pool Order [{}] updated, Items included: {}, Items excluded: {}, total events sent: {}",
            orderId, included, excluded, messages.size
        )
        orderEventService.updatePoolOrder(order, eventTimeMarks)
    }

    private suspend fun fetchOrder(orderId: OrderIdDto): UnionOrder {
        // Since there could be delay of message delivery, it's better to re-fetch order
        // to have it in actual state
        return enrichmentOrderService.getById(orderId)
            ?: throw UnionNotFoundException("Order [{}] not found in blockchain")
    }

}