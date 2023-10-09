package com.rarible.protocol.union.listener.handler.cmp

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceChangeEventDto
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceDeleteEventDto
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceEventDto
import com.rarible.protocol.union.enrichment.converter.MarketplaceConverter
import com.rarible.protocol.union.enrichment.model.Marketplace
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import org.springframework.stereotype.Component

@Component
class CommunityMarketplaceEventHandler(
    private val collectionRepository: CollectionRepository,
) : RaribleKafkaBatchEventHandler<MarketplaceEventDto> {
    override suspend fun handle(events: List<MarketplaceEventDto>) {
        events.forEach { event ->
            when (event) {
                is MarketplaceChangeEventDto -> handleUpdate(MarketplaceConverter.convert(event.data))
                is MarketplaceDeleteEventDto -> handleDelete(MarketplaceConverter.convert(event.data))
            }
        }
    }

    private suspend fun handleUpdate(marketplace: Marketplace) {
        collectionRepository.updatePriority(
            collectionIds = marketplace.collectionIds,
            priority = marketplace.metaRefreshPriority,
        )
    }

    private suspend fun handleDelete(marketplace: Marketplace) {
        collectionRepository.updatePriority(collectionIds = marketplace.collectionIds, priority = null)
    }
}
