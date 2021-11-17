package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.event.OutgoingOrderEventListener
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.ContractAddress
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
class EnrichmentAuctionEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateAuction(auction: AuctionDto, notificationEnabled: Boolean = true) = coroutineScope {
        val blockchain = auction.id.blockchain
        val makeAssetExt = auction.sell.type.ext
        val makeItemIdDto = makeAssetExt.itemId(blockchain)
        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }

        if (makeItemId != null) {
            ignoreApi404 {
                enrichmentItemEventService.onAuctionUpdated(makeItemId, auction, notificationEnabled)
            }
        }
    }

    suspend fun deleteAuction(auctionId: AuctionIdDto, notificationEnabled: Boolean = true) = coroutineScope {
//        val blockchain = auction.id.blockchain
//        val makeAssetExt = auction.sell.type.ext
//        val makeItemIdDto = makeAssetExt.itemId(blockchain)
//        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }
//
//        if (makeItemId != null) {
//            ignoreApi404 {
//                enrichmentItemEventService.onAuctionDeleted(makeItemId, auction, notificationEnabled)
//            }
//        }
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
        }
    }
}
