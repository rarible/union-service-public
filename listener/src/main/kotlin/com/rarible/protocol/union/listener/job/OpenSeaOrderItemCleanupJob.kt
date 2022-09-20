package com.rarible.protocol.union.listener.job

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class OpenSeaOrderItemCleanupJob(
    private val itemRepository: ItemRepository,
    private val itemService: EnrichmentItemService,
    private val itemEventListeners: List<OutgoingItemEventListener>,
    private val orderFilter: OpenSeaCleanupOrderFilter,
    private val properties: UnionListenerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = properties.openSeaCleanup.itemBatchSize
    private val from = properties.openSeaCleanup.sellOrderFrom
    private val enabled = properties.openSeaCleanup.enabled

    fun execute(fromShortItemId: ShortItemId?): Flow<ShortItemId> {
        if (!enabled) {
            return emptyFlow()
        }
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

        if (orderFilter.isOld(item.blockchain, openSeaOrder.id, from)) {
            return
        }

        val updated = item.copy(bestSellOrder = null, bestSellOrders = emptyMap())

        logger.info("Updated item [{}], OpenSea order removed: [{}]", updated, openSeaOrder.id)
        itemRepository.save(updated)

        val dto = itemService.enrichItem(
            shortItem = updated,
            metaPipeline = ItemMetaPipeline.SYNC
        )

        ignoreApi404 {
            val event = ItemUpdateEventDto(
                itemId = dto.id,
                item = dto,
                eventId = UUID.randomUUID().toString()
            )

            itemEventListeners.forEach { it.onEvent(event) }
        }
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn(
                "Received NOT_FOUND code from client during item update: {}, message: {}", ex.data, ex.message
            )
        }
    }
}