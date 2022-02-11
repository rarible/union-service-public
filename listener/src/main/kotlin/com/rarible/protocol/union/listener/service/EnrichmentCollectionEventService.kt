package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionEventService(
    private val itemService: EnrichmentItemService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentCollectionEventService::class.java)
    private val concurrency = 4

    suspend fun onCollectionBestSellOrderUpdate(
        collectionId: CollectionIdDto,
        order: OrderDto,
        notificationEnabled: Boolean
    ) = coroutineScope {
        itemService.findByCollection(collectionId, order.maker)
            .map { item ->
                async {
                    ignoreApi404 {
                        enrichmentItemEventService.onItemBestSellOrderUpdated(item, order, notificationEnabled)
                    }
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
            }.buffer(concurrency).map { it.await() }.collect()
    }

    suspend fun onCollectionBestBidOrderUpdate(
        collectionId: CollectionIdDto,
        order: OrderDto,
        notificationEnabled: Boolean
    ) = coroutineScope {
        itemService.findByCollection(collectionId).map { item ->
            async {
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestBidOrderUpdated(item, order, notificationEnabled)
                }
            }
        }.buffer(concurrency).map { it.await() }.collect()
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn(
                    "Received NOT_FOUND code from client during Collection update, details: {}, message: {}",
                    ex.data,
                    ex.message
                )
            } else {
                throw ex
            }
        }
    }
}
