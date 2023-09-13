package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PlatformBestSellOrderOwnershipCleanupJob(
    private val ownershipRepository: OwnershipRepository,
    private val internalOwnershipEventProducer: UnionInternalOwnershipEventProducer,
    properties: WorkerProperties
) : AbstractBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = properties.platformBestSellCleanup.itemBatchSize
    private val enabled = properties.platformBestSellCleanup.enabled

    override suspend fun handleBatch(continuation: String?, param: String): String? {
        if (!enabled) return null
        val state = continuation?.let { DateIdContinuation.parse(it) }

        val batch = ownershipRepository.findByPlatformWithSell(
            platform = PlatformDto.valueOf(param),
            fromLastUpdatedAt = state?.date ?: Instant.now(),
            fromOwnershipId = state?.id?.let { ShortOwnershipId.of(it) },
            limit = batchSize
        ).toList()

        coroutineScope {
            batch.map {
                async { cleanup(it) }
            }.awaitAll()
        }
        val next = batch.lastOrNull()?.let { DateIdContinuation(it.lastUpdatedAt, it.id.toString()) }?.toString()
        logger.info("CleanedUp {} OpenSea ownerships, last state: {}", batch.size, next)
        return next
    }

    private suspend fun cleanup(ownership: ShortOwnership) {
        val order = ownership.bestSellOrder ?: return

        val updated = ownership.copy(bestSellOrder = null, bestSellOrders = emptyMap())

        logger.info("Updated ownership {}, OpenSea order removed: {}", updated, order.id)
        ownershipRepository.save(updated.withCalculatedFields())

        internalOwnershipEventProducer.sendChangeEvent(updated.id.toDto())
    }
}
