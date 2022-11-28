package com.rarible.protocol.union.enrichment.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentOrderEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService,
    private val orderEventListeners: List<OutgoingEventListener<OrderEventDto>>,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updatePoolOrder(
        order: OrderDto,
        itemId: ItemIdDto,
        action: PoolItemAction,
        notificationEnabled: Boolean = true
    ) {
        // ATM we support only sell-orders for AMM
        // Order event should not be sent here
        // Ownership updates doesn't seem reasonable here too

        val shortItemId = ShortItemId(itemId)
        onItemPoolOrder(order, shortItemId, action, notificationEnabled)
        onOwnershipPoolOrder(order, shortItemId, action, notificationEnabled)
    }

    suspend fun updateOrder(order: OrderDto, notificationEnabled: Boolean = true) {

        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val makeItemIdDto = makeAssetExt.itemId
        val takeItemIdDto = takeAssetExt.itemId

        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }
        val takeItemId = takeItemIdDto?.let { ShortItemId(it) }

        when {
            // TODO PT-1151 Maybe we need to ensure there is AMM order? originally they should not get here
            // Floor sell (not present ATM)
            makeAssetExt.isCollection -> onCollectionSellOrder(order, makeAssetExt.collectionId!!, notificationEnabled)
            // Floor bid
            takeAssetExt.isCollection -> onCollectionBidOrder(order, takeAssetExt.collectionId!!, notificationEnabled)
            // Direct sell order
            (makeItemId != null && takeItemId == null) -> {
                // Item should be checked first, otherwise ownership could trigger event for outdated item
                onItemSellOrder(order, makeItemId, notificationEnabled)
                onOwnershipSellOrder(order, makeItemId, notificationEnabled)
            }
            // Direct bid order
            (makeItemId == null && takeItemId != null) -> onItemBidOrder(order, takeItemId, notificationEnabled)
            // Something else, like swap order (NFT to NFT)
            else -> logger.info("Unsupported Order's asset combination: $order")
        }

        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = order.id,
            order = order
        )
        orderEventListeners.forEach { it.onEvent(event) }
    }

    private suspend fun onItemSellOrder(
        order: OrderDto,
        itemId: ShortItemId,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        enrichmentItemEventService.onItemBestSellOrderUpdated(itemId, order, notificationEnabled)
    }

    private suspend fun onItemPoolOrder(
        order: OrderDto,
        itemId: ShortItemId,
        action: PoolItemAction,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        enrichmentItemEventService.onPoolOrderUpdated(itemId, order, action, notificationEnabled)
    }

    private suspend fun onOwnershipSellOrder(
        order: OrderDto,
        itemId: ShortItemId,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        val ownershipId = ShortOwnershipId(itemId.blockchain, itemId.itemId, order.maker.value)
        enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(ownershipId, order, notificationEnabled)
    }

    private suspend fun onOwnershipPoolOrder(
        order: OrderDto,
        itemId: ShortItemId,
        action: PoolItemAction,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        val ownershipId = ShortOwnershipId(itemId.blockchain, itemId.itemId, order.maker.value)
        enrichmentOwnershipEventService.onPoolOrderUpdated(ownershipId, order, action, notificationEnabled)
    }

    private suspend fun onItemBidOrder(
        order: OrderDto,
        itemId: ShortItemId,
        notificationEnabled: Boolean
    ) = ignoreApi404 {
        enrichmentItemEventService.onItemBestBidOrderUpdated(itemId, order, notificationEnabled)
    }

    private suspend fun onCollectionSellOrder(
        order: OrderDto,
        collectionId: CollectionIdDto,
        notificationEnabled: Boolean
    ) {
        val withNotification = notificationEnabled && ff.enableNotificationOnCollectionOrders
        enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(
            collectionId,
            order,
            withNotification
        )
    }

    private suspend fun onCollectionBidOrder(
        order: OrderDto,
        collectionId: CollectionIdDto,
        notificationEnabled: Boolean
    ) {
        val withNotification = notificationEnabled && ff.enableNotificationOnCollectionOrders
        enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(
            collectionId,
            order,
            withNotification
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
