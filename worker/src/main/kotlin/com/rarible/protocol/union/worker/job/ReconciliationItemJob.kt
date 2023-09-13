package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ReconciliationItemJob(
    private val itemRepository: ItemRepository,
    private val refreshService: EnrichmentRefreshService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun handle(continuation: String?, param: String): Flow<String> {
        val state = continuation?.let { DateIdContinuation.parse(it) }
        return itemRepository.findByPlatformWithSell(
            platform = PlatformDto.valueOf(param),
            fromLastUpdatedAt = state?.date ?: Instant.now(),
            fromItemId = state?.id?.let { ShortItemId.of(it) },
            limit = null
        ).onEach { shortItem ->
            refreshService.reconcileItem(shortItem.id.toDto(), full = true)
            logger.info("Item ${shortItem.id} was reconciled, bestSellOrder=${shortItem.bestSellOrder}")
        }.map { DateIdContinuation(it.lastUpdatedAt, it.id.toString()).toString() }
    }
}
