package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionEventService(
    private val itemService: EnrichmentItemService,
    private val enrichmentItemEventService: EnrichmentItemEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentCollectionEventService::class.java)

    suspend fun onCollectionBestSellOrderUpdate(address: UnionAddress, order: OrderDto, notificationEnabled: Boolean = true) {
        val items = itemService.findByAddress(address)
        items.forEach { item ->
            ignoreApi404 {
                enrichmentItemEventService.onItemBestSellOrderUpdated(item.id, order, notificationEnabled)
            }
        }
    }

    suspend fun onCollectionBestBidOrderUpdate(address: UnionAddress, order: OrderDto, notificationEnabled: Boolean = true) {
        val items = itemService.findByAddress(address)
        items.forEach { item ->
            ignoreApi404 {
                enrichmentItemEventService.onItemBestBidOrderUpdated(item.id, order, notificationEnabled)
            }
        }
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
        }
    }
}
