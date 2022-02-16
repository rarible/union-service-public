package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class OpenSeaOrderCleanupJob(
    private val itemRepository: ItemRepository,
    private val itemService: EnrichmentItemService,
    private val itemEventListeners: List<OutgoingItemEventListener>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 10

    fun execute(fromShortItemId: ShortItemId?): Flow<ShortItemId> {
        return flow {
            var next = fromShortItemId
            do {
                next = cleanup(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    suspend fun cleanup(fromShortItemId: ShortItemId?): ShortItemId? {
        val batch = itemRepository.findByPlatformWithSell(PlatformDto.OPEN_SEA, fromShortItemId, batchSize)
            .toList()

        coroutineScope {
            batch.map {
                async { cleanup(it) }
            }.awaitAll()
        }
        val next = batch.lastOrNull()?.id
        logger.info("CleanedUp {} OpenSea items, last ItemId: [{}]", batch.size, next)
        return next
    }

    private suspend fun cleanup(item: ShortItem) {
        val openSeaOrder = item.bestSellOrder ?: return

        val updated = item.copy(bestSellOrder = null, bestSellOrders = emptyMap())

        if (updated.isNotEmpty()) {
            logger.info("Updated item [{}], OpenSea order removed: [{}]", updated, openSeaOrder.id)
            itemRepository.save(updated)
        } else {
            logger.info("Deleted enriched item [{}], OpenSea order removed: [{}]", updated, openSeaOrder.id)
            itemRepository.delete(item.id)
        }

        val dto = itemService.enrichItem(updated)

        val event = ItemUpdateEventDto(
            itemId = dto.id,
            item = dto,
            eventId = UUID.randomUUID().toString()
        )

        itemEventListeners.forEach { it.onEvent(event) }
    }
}