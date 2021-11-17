package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentAuctionEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateAuction(auction: AuctionDto, notificationEnabled: Boolean = true) = coroutineScope {
        val makeAssetExt = auction.sell.type.ext
        val makeItemIdDto = makeAssetExt.itemId
        val makeItemId = makeItemIdDto?.let { ShortItemId(it) }

        val iFuture = makeItemId?.let {
            async {
                ignoreApi404 {
                    enrichmentItemEventService.onAuctionUpdated(it, auction, notificationEnabled)
                }
            }
        }

        val oFuture = makeItemId?.let {
            val ownershipId = ShortOwnershipId(
                makeItemId.blockchain,
                makeItemId.token,
                makeItemId.tokenId,
                auction.seller.value
            )
            async {
                ignoreApi404 {
                    enrichmentOwnershipEventService.onAuctionUpdated(
                        ownershipId,
                        auction,
                        notificationEnabled
                    )
                }
            }
        }

        iFuture?.await()
        oFuture?.await()
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
