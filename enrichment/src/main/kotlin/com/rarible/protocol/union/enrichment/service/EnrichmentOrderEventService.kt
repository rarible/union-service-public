package com.rarible.protocol.union.enrichment.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EnrichmentOrderEventService(
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService,
    private val orderEventListeners: List<OutgoingEventListener<OrderEventDto>>,
    private val ff: FeatureFlagsProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updatePoolOrder(
        order: UnionOrder,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {
        // There is no actions for original pool order update event,
        // so we just pass through original event to the event queue
        notify(order, eventTimeMarks)
    }

    suspend fun updatePoolOrderPerItem(
        order: UnionOrder,
        itemId: ItemIdDto,
        action: PoolItemAction,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {
        // ATM we support only sell-orders for AMM
        // Order event should not be sent here
        // Ownership updates doesn't seem reasonable here too

        val shortItemId = ShortItemId(itemId)
        onItemPoolOrder(order, shortItemId, action, eventTimeMarks, notificationEnabled)
        onOwnershipPoolOrder(order, shortItemId, action, eventTimeMarks, notificationEnabled)

        // Order event should not be passed through here
        // because we apply update of same order for each item in the pool
    }

    suspend fun updateOrder(
        order: UnionOrder,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {

        val makeAssetExt = order.make.type
        val takeAssetExt = order.take.type

        val makeItemIdDto = makeAssetExt.itemId()
        val takeItemIdDto = takeAssetExt.itemId()

        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }
        val takeItemId = takeItemIdDto?.let { ShortItemId(it) }

        when {
            // TODO PT-1151 Maybe we need to ensure there is AMM order? originally they should not get here
            // Floor sell (not present ATM)
            makeAssetExt.isCollectionAsset() -> onCollectionSellOrder(
                order,
                makeAssetExt.collectionId()!!,
                eventTimeMarks,
                notificationEnabled
            )
            // Floor bid
            takeAssetExt.isCollectionAsset() -> onCollectionBidOrder(
                order,
                takeAssetExt.collectionId()!!,
                eventTimeMarks,
                notificationEnabled
            )
            // Direct sell order
            (makeItemId != null && takeItemId == null) -> {
                // Item should be checked first, otherwise ownership could trigger event for outdated item
                onItemSellOrder(order, makeItemId, eventTimeMarks, notificationEnabled)
                onOwnershipSellOrder(order, makeItemId, eventTimeMarks, notificationEnabled)
            }
            // Direct bid order
            (makeItemId == null && takeItemId != null) -> onItemBidOrder(
                order,
                takeItemId,
                eventTimeMarks,
                notificationEnabled
            )
            // Something else, like swap order (NFT to NFT)
            else -> logger.info("Unsupported Order's asset combination: $order")
        }

        notify(order, eventTimeMarks)
    }

    private suspend fun notify(order: UnionOrder, eventTimeMarks: UnionEventTimeMarks?) {
        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = order.id,
            order = enrichmentOrderService.enrich(order),
            eventTimeMarks = eventTimeMarks?.addOut()?.toDto()
        )
        orderEventListeners.forEach { it.onEvent(event) }
    }

    private suspend fun onItemSellOrder(
        order: UnionOrder,
        itemId: ShortItemId,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        enrichmentItemEventService.onItemBestSellOrderUpdated(itemId, order, eventTimeMarks, notificationEnabled)
    }

    private suspend fun onItemPoolOrder(
        order: UnionOrder,
        itemId: ShortItemId,
        action: PoolItemAction,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        enrichmentItemEventService.onPoolOrderUpdated(
            itemId,
            order,
            action,
            eventTimeMarks,
            notificationEnabled
        )
    }

    private suspend fun onOwnershipSellOrder(
        order: UnionOrder,
        itemId: ShortItemId,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        val ownershipId = ShortOwnershipId(itemId.blockchain, itemId.itemId, order.maker.value)
        enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(
            ownershipId,
            order,
            eventTimeMarks,
            notificationEnabled
        )
    }

    private suspend fun onOwnershipPoolOrder(
        order: UnionOrder,
        itemId: ShortItemId,
        action: PoolItemAction,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        val ownershipId = ShortOwnershipId(itemId.blockchain, itemId.itemId, order.maker.value)
        enrichmentOwnershipEventService.onPoolOrderUpdated(
            ownershipId,
            order,
            action,
            eventTimeMarks,
            notificationEnabled
        )
    }

    private suspend fun onItemBidOrder(
        order: UnionOrder,
        itemId: ShortItemId,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        enrichmentItemEventService.onItemBestBidOrderUpdated(itemId, order, eventTimeMarks, notificationEnabled)
    }

    private suspend fun onCollectionSellOrder(
        order: UnionOrder,
        collectionId: CollectionIdDto,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) {
        enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(
            collectionId,
            order,
            eventTimeMarks,
            notificationEnabled
        )
    }

    private suspend fun onCollectionBidOrder(
        order: UnionOrder,
        collectionId: CollectionIdDto,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) {
        enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(
            collectionId,
            order,
            eventTimeMarks,
            notificationEnabled
        )
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn(
                    "Received NOT_FOUND code from client during Order update, details: {}, message: {}",
                    ex.data,
                    ex.message
                )
            } else {
                throw ex
            }
        }
    }
}
