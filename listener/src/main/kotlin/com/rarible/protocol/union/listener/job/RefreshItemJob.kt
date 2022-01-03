package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Component

@Component
class RefreshItemJob(
    private val itemRepository: ItemRepository,
    private val refreshService: EnrichmentRefreshService
) {
    fun refreshForPlatform(platform: String, fromShortItemId: ShortItemId?): Flow<ShortItemId> {
        return itemRepository.findWithSellAndPlatform(platform, fromShortItemId)
            .onEach { shortItem -> refreshService.reconcileItem(shortItem.id.toDto(), full = true) }
            .map { shortItem -> shortItem.id }
    }
}