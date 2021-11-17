package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentAuctionEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateAuction(auction: AuctionDto, notificationEnabled: Boolean = true) = coroutineScope {
        val makeAssetExt = auction.sell.type.ext
        val makeItemIdDto = makeAssetExt.itemId
        makeItemIdDto?.let {
            ignoreApi404 {
                enrichmentItemEventService.onAuctionUpdated(ShortItemId(it), auction, notificationEnabled)
            }
        }
    }

    suspend fun deleteAuction(auctionId: AuctionIdDto, notificationEnabled: Boolean = true) = coroutineScope {
        ignoreApi404 {
            enrichmentItemEventService.onAuctionDeleted(auctionId, notificationEnabled)
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
