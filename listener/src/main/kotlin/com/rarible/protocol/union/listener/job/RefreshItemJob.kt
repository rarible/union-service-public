package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RefreshItemJob(
    private val itemRepository: ItemRepository,
    private val refreshService: EnrichmentRefreshService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun refreshForPlatform(platform: String, fromShortItemId: ShortItemId?): Flow<ShortItemId> {
        return itemRepository.findWithSellAndPlatform(platform, fromShortItemId)
            .onEach { shortItem ->
                refreshService.reconcileItem(shortItem.id.toDto(), full = true)
                logger.info("Item ${shortItem.id} was reconciled, bestSellOrder=${shortItem.bestSellOrder}")
            }
            .map { shortItem -> shortItem.id }
    }
}