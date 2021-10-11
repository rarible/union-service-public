package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PriceUpdateJob(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    properties: UnionListenerProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val updateRate = properties.priceUpdate.rate

    @Scheduled(
        fixedRateString = "\${listener.price-update.rate}",
        initialDelayString = "\${listener.price-update.delay}"
    )
    fun updateBestOrderPrice() = runBlocking<Unit> {
        val notUpdatedSince = nowMillis() - updateRate

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
}
