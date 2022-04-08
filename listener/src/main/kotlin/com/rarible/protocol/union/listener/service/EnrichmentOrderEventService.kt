package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    suspend fun updateOrder(order: OrderDto, notificationEnabled: Boolean = true) = coroutineScope {

        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val makeItemIdDto = makeAssetExt.itemId
        val takeItemIdDto = takeAssetExt.itemId

        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }
        val takeItemId = takeItemIdDto?.let { ShortItemId(it) }

        val sellUpdateFuture = makeItemId?.let {
            async {
                if (!makeAssetExt.isCollection) {
                    // Item should be checked first, otherwise ownership could trigger event for outdated item
                    ignoreApi404 {
                        enrichmentItemEventService.onItemBestSellOrderUpdated(makeItemId, order, notificationEnabled)
                    }
                    ignoreApi404 {
                        val ownershipId = ShortOwnershipId(
                            makeItemId.blockchain, makeItemId.itemId, order.maker.value
                        )
                        enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(
                            ownershipId, order, notificationEnabled
                        )
                    }
                }
            }
        }
        val bidUpdateFuture = takeItemId?.let {
            async {
                if (!makeAssetExt.isCollection) {
                    ignoreApi404 {
                        enrichmentItemEventService.onItemBestBidOrderUpdated(takeItemId, order, notificationEnabled)
                    }
                }
            }
        }

        val sellCollectionUpdateFuture = if (makeAssetExt.isCollection) {
            async {
                val collectionId = makeAssetExt.collectionId!!
                enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(
                    collectionId, order, notificationEnabled && ff.enableNotificationOnCollectionOrders
                )
            }
        } else null

        val bidCollectionUpdateFuture = if (takeAssetExt.isCollection) {
            async {
                val collectionId = takeAssetExt.collectionId!!
                enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(
                    collectionId, order, notificationEnabled && ff.enableNotificationOnCollectionOrders
                )
            }
        } else null

        sellUpdateFuture?.await()
        bidUpdateFuture?.await()
        sellCollectionUpdateFuture?.await()
        bidCollectionUpdateFuture?.await()

        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = order.id,
            order = order
        )
        orderEventListeners.forEach { it.onEvent(event) }
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
