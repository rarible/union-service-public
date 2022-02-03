package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.AuctionDto
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class EnrichmentAuctionEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onAuctionUpdated(auction: AuctionDto) = coroutineScope {
        ignoreApi404 { enrichmentItemEventService.onAuctionUpdated(auction) }
        enrichmentOwnershipEventService.onAuctionUpdated(auction)

    }

    suspend fun onAuctionDeleted(auction: AuctionDto) = coroutineScope {
        ignoreApi404 { enrichmentItemEventService.onAuctionDeleted(auction) }
        enrichmentOwnershipEventService.onAuctionDeleted(auction)
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn(
                    "Received NOT_FOUND code from client during Auction update, details: {}, message: {}",
                    ex.data,
                    ex.message
                )
            } else {
                throw ex
            }
        }
    }
}
