package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionAuctionDeleteEvent
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionAuctionUpdateEvent
import com.rarible.protocol.union.core.model.getItemId
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionEventService
import com.rarible.protocol.union.enrichment.service.ReconciliationEventService
import org.springframework.stereotype.Component

@Component
class UnionInternalAuctionEventHandler(
    private val auctionEventService: EnrichmentAuctionEventService,
    private val reconciliationEventService: ReconciliationEventService
) {

    @CaptureTransaction("UnionAuctionEvent")
    suspend fun onEvent(event: UnionAuctionEvent) {
        try {
            when (event) {
                is UnionAuctionUpdateEvent -> {
                    auctionEventService.onAuctionUpdated(event.auction)
                }
                is UnionAuctionDeleteEvent -> {
                    auctionEventService.onAuctionDeleted(event.auction)
                }
            }
        } catch (e: Throwable) {
            reconciliationEventService.onCorruptedItem(event.auction.getItemId())
            reconciliationEventService.onCorruptedOwnership(event.auction.getSellerOwnershipId())
            throw e
        }
    }
}
