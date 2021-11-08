package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionEventService(
    private val itemService: EnrichmentItemService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {

    suspend fun onCollectionBestSellOrderUpdate(address: UnionAddress, order: OrderDto, notificationEnabled: Boolean) = coroutineScope {
        itemService.findByAddress(address).buffer(BUFFER_SIZE).map { item ->
            val bFuture = async {
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestSellOrderUpdated(item.id, order, notificationEnabled)
                }
            }
            val oFuture = async {
                val ownershipId = ShortOwnershipId(
                    item.blockchain,
                    item.token,
                    item.tokenId,
                    order.maker.value
                )
                ignoreApi404 {
                    enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(
                        ownershipId,
                        order,
                        notificationEnabled
                    )
                }
            }
            listOf(bFuture, oFuture).awaitAll()
        }.collect()
    }

    suspend fun onCollectionBestBidOrderUpdate(address: UnionAddress, order: OrderDto, notificationEnabled: Boolean) {
        itemService.findByAddress(address).buffer(BUFFER_SIZE).map { item ->
            ignoreApi404 {
                enrichmentItemEventService.onItemBestBidOrderUpdated(item.id, order, notificationEnabled)
            }
        }.collect()
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 4
        private val logger = LoggerFactory.getLogger(EnrichmentCollectionEventService::class.java)
    }
}
