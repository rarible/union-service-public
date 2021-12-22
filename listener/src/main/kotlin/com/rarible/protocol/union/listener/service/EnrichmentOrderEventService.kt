package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.event.OutgoingOrderEventListener
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
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

    suspend fun updateOrder(order: OrderDto, notificationEnabled: Boolean = true, ownershipEnabled: Boolean = true) = coroutineScope {
        val blockchain = order.id.blockchain
        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val makeItemIdDto = makeAssetExt.itemId
        val takeItemIdDto = takeAssetExt.itemId

        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }
        val takeItemId = takeItemIdDto?.let { ShortItemId(it) }

        val sellUpdateFuture = makeItemId?.let {
            async {
                // MP-2267
                // We send duplicate ownership event:
                // 1. The first time we send ownership event here (for example we could send with wrong value)
                // 2. Then we proxy ownership event from blockchains to marketplace after we get ownership event
                if (ownershipEnabled) {
                    // Ownership should be updated first in order to emit events in correct order
                    // Otherwise there could be situation when Order for item changed, but ownership triggers item
                    // event caused by stock re-evaluation later than order for item updated
                    ignoreApi404 {
                        val ownershipId = ShortOwnershipId(
                            makeItemId.blockchain, makeItemId.token, makeItemId.tokenId, order.maker.value
                        )
                        enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(
                            ownershipId, order, notificationEnabled
                        )
                    }
                }
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestSellOrderUpdated(makeItemId, order, notificationEnabled)
                }
            }
        }
        val bidUpdateFuture = takeItemId?.let {
            async {
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestBidOrderUpdated(takeItemId, order, notificationEnabled)
                }
            }
        }

        val sellCollectionUpdateFuture = if (order.make.type.ext.isCollection) {
            async {
                val collectionId = ContractAddressConverter.convert(blockchain, makeAssetExt.contract)
                enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(
                    collectionId, order, notificationEnabled
                )
            }
        } else null

        val bidCollectionUpdateFuture = if (order.take.type.ext.isCollection) {
            async {
                val address = ContractAddressConverter.convert(blockchain, takeAssetExt.contract)
                enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(address, order, notificationEnabled)
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
            logger.warn(
                "Received NOT_FOUND code from client during order update: {}, message: {}", ex.data, ex.message
            )
        }
    }

}
