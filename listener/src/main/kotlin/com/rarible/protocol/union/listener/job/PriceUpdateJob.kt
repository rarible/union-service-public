package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class PriceUpdateJob(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun updateBestOrderPrice() = runBlocking<Unit> {
        val notUpdatedSince = nowMillis() - UPDATE_PERIOD

        itemRepository.findWithMultiCurrency(notUpdatedSince).collect { shortItemId ->
            logger.info("Update price for item {}", shortItemId)
            enrichmentItemEventService.onItemBestSellOrdersPriceUpdate(shortItemId)
            enrichmentItemEventService.onItemBestBidOrdersPriceUpdated(shortItemId)
        }
        ownershipRepository.findWithMultiCurrency(notUpdatedSince).collect { shortOwnershipId ->
            logger.info("Update price for ownership {}", shortOwnershipId)
            enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(shortOwnershipId)
        }
    }

    companion object {
        val UPDATE_PERIOD: Duration = Duration.ofMinutes(5)
    }
}
