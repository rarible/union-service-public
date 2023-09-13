package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.worker.config.WorkerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PlatformBestSellOrderItemCleanupJob(
    private val itemRepository: ItemRepository,
    private val itemService: EnrichmentItemService,
    private val internalItemEventProducer: UnionInternalItemEventProducer,
    properties: WorkerProperties
) : AbstractBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = properties.platformBestSellCleanup.itemBatchSize
    private val enabled = properties.platformBestSellCleanup.enabled

    override suspend fun handleBatch(continuation: String?, param: String): String? {
        if (!enabled) return null
        val state = continuation?.let { DateIdContinuation.parse(it) }

        val batch = itemRepository.findByPlatformWithSell(
            platform = PlatformDto.valueOf(param),
            fromLastUpdatedAt = state?.date ?: Instant.now(),
            fromItemId = state?.id?.let { ShortItemId.of(it) },
            limit = batchSize
        ).toList()

        coroutineScope {
            batch.map {
                async { cleanup(it) }
            }.awaitAll()
        }
        val next = batch.lastOrNull()?.let { DateIdContinuation(it.lastUpdatedAt, it.id.toString()) }?.toString()
        logger.info("CleanedUp {} OpenSea items, last state: {}", batch.size, next)
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

        internalItemEventProducer.sendChangeEvent(dto.id)
    }
}
