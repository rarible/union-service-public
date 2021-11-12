package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.event.OutgoingOrderEventListener
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentOrderEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService,
    private val orderEventListeners: List<OutgoingOrderEventListener>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateOrder(order: OrderDto, notificationEnabled: Boolean = true) = coroutineScope {
        val makeItemIdDto = order.make.type.ext.itemId
        val takeItemIdDto = order.take.type.ext.itemId

        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }
        val takeItemId = takeItemIdDto?.let { ShortItemId(it) }

        val mFuture = makeItemId?.let {
            async {
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestSellOrderUpdated(makeItemId, order, notificationEnabled)
                }
            }
        }
        val tFuture = takeItemId?.let {
            async {
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestBidOrderUpdated(takeItemId, order, notificationEnabled)
                }
            }
        }
        val oFuture = makeItemId?.let {
            val ownershipId = ShortOwnershipId(
                makeItemId.blockchain,
                makeItemId.token,
                makeItemId.tokenId,
                order.maker.value
            )
            async {
                ignoreApi404 {
                    enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(
                        ownershipId,
                        order,
                        notificationEnabled
                    )
                }
            }
        }
        val mcFuture = if (order.make.type.ext.isCollection) {
            async {
                val address = UnionAddress(order.id.blockchain, order.make.type.ext.contract)
                enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(address, order, notificationEnabled)
            }
        } else null
        val tcFuture = if (order.take.type.ext.isCollection) {
            async {
                val address = UnionAddress(order.id.blockchain, order.take.type.ext.contract)
                enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(address, order, notificationEnabled)
            }
        } else null

        mFuture?.await()
        tFuture?.await()
        oFuture?.await()
        mcFuture?.await()
        tcFuture?.await()
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
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
        }
    }

}
