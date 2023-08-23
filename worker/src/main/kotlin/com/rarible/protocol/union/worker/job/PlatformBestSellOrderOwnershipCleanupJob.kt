package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
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
class PlatformBestSellOrderOwnershipCleanupJob(
    private val ownershipRepository: OwnershipRepository,
    private val internalOwnershipEventProducer: UnionInternalOwnershipEventProducer,
    properties: WorkerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = properties.platformBestSellCleanup.itemBatchSize
    private val enabled = properties.platformBestSellCleanup.enabled

    fun execute(platform: PlatformDto, fromShortOwnershipId: ShortOwnershipId?): Flow<ShortOwnershipId> {
        if (!enabled) {
            return emptyFlow()
        }
        return flow {
            var next = fromShortOwnershipId
            do {
                next = cleanup(platform, next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    suspend fun cleanup(platform: PlatformDto, fromShortOwnershipId: ShortOwnershipId?): ShortOwnershipId? {
        val batch = ownershipRepository.findByPlatformWithSell(platform, fromShortOwnershipId, batchSize)
            .toList()

        coroutineScope {
            batch.map {
                async { cleanup(it) }
            }.awaitAll()
        }
        val next = batch.lastOrNull()?.id
        logger.info("CleanedUp {} OpenSea ownerships, last ownershipId: [{}]", batch.size, next)
        return next
    }

    private suspend fun cleanup(ownership: ShortOwnership) {
        val order = ownership.bestSellOrder ?: return

        val updated = ownership.copy(bestSellOrder = null, bestSellOrders = emptyMap())

        logger.info("Updated ownership [{}], OpenSea order removed: [{}]", updated, order.id)
        ownershipRepository.save(updated.withCalculatedFields())

        internalOwnershipEventProducer.sendChangeEvent(updated.id.toDto())
    }
}
