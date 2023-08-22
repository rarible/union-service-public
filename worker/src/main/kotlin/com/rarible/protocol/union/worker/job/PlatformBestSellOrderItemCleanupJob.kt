package com.rarible.protocol.union.worker.job

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.worker.config.WorkerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PlatformBestSellOrderItemCleanupJob(
    private val itemRepository: ItemRepository,
    private val itemService: EnrichmentItemService,
    private val internalItemEventProducer: UnionInternalItemEventProducer,
    properties: WorkerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = properties.platformBestSellCleanup.itemBatchSize
    private val enabled = properties.platformBestSellCleanup.enabled

    fun execute(platform: PlatformDto, fromShortItemId: ShortItemId?): Flow<ShortItemId> {
        if (!enabled) {
            return emptyFlow()
        }
        return flow {
            var next = fromShortItemId
            do {
                next = cleanup(platform, next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    suspend fun cleanup(platform: PlatformDto, fromShortItemId: ShortItemId?): ShortItemId? {
        val batch = itemRepository.findByPlatformWithSell(platform, fromShortItemId, batchSize)
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
        val order = item.bestSellOrder ?: return

        val updated = item.copy(bestSellOrder = null, bestSellOrders = emptyMap())

        logger.info("Updated item [{}], OpenSea order removed: [{}]", updated, order.id)
        itemRepository.save(updated.withCalculatedFields())

        val dto = itemService.enrichItem(
            shortItem = updated,
            metaPipeline = ItemMetaPipeline.SYNC
        )

        ignoreApi404 {
            internalItemEventProducer.sendChangeEvent(dto.id)
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
