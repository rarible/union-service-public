package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class PriceUpdateJob(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val enrichmentItemEventService: EnrichmentItemEventService
) {
    fun updateBestOrderPrice() = runBlocking<Unit> {
        val notUpdatedSince = nowMillis() - UPDATE_PERIOD

        itemRepository.findWithMultiCurrency(notUpdatedSince).collect { shortItemId ->
            enrichmentItemEventService.onItemBestSellOrdersPriceUpdate(shortItemId)
            enrichmentItemEventService.onItemBestBidOrdersPriceUpdated(shortItemId)
        }
        ownershipRepository.findWithMultiCurrency(notUpdatedSince).collect { shortOwnershipId ->
            enrichmentItemEventService.onOwnershipUpdated(shortOwnershipId)
        }
    }

    companion object {
        val UPDATE_PERIOD: Duration = Duration.ofMinutes(5)
    }
}
