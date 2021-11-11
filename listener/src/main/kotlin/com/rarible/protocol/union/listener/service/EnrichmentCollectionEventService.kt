package com.rarible.protocol.union.listener.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
@CaptureSpan(type = "event", subtype = "collection")
class EnrichmentCollectionEventService(
    private val itemService: EnrichmentItemService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentCollectionEventService::class.java)
    private val threadPoolSize = 4
    private val threadPool = Executors.newFixedThreadPool(threadPoolSize)
    private val dispatcher = threadPool.asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)

    suspend fun onCollectionBestSellOrderUpdate(address: UnionAddress, order: OrderDto, notificationEnabled: Boolean) {
        itemService.findByCollection(address, order.maker)
            .map { item ->
                scope.async {
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
            }.buffer(threadPoolSize).map { it.await() }.flowOn(dispatcher).collect()
    }

    suspend fun onCollectionBestBidOrderUpdate(address: UnionAddress, order: OrderDto, notificationEnabled: Boolean) {
        itemService.findByCollection(address).map { item ->
            scope.async {
                ignoreApi404 {
                    enrichmentItemEventService.onItemBestBidOrderUpdated(item, order, notificationEnabled)
                }
            }
        }.buffer(threadPoolSize).map { it.await() }.flowOn(dispatcher).collect()
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
        }
    }
}
