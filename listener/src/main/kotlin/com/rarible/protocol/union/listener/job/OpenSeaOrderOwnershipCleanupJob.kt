package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
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
class OpenSeaOrderOwnershipCleanupJob(
    private val ownershipRepository: OwnershipRepository,
    private val ownershipService: EnrichmentOwnershipService,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 50

    fun execute(fromShortOwnershipId: ShortOwnershipId?): Flow<ShortOwnershipId> {
        return flow {
            var next = fromShortOwnershipId
            do {
                next = cleanup(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    suspend fun cleanup(fromShortOwnershipId: ShortOwnershipId?): ShortOwnershipId? {
        val batch = ownershipRepository.findByPlatformWithSell(PlatformDto.OPEN_SEA, fromShortOwnershipId, batchSize)
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
        val openSeaOrder = ownership.bestSellOrder ?: return

        val updated = ownership.copy(bestSellOrder = null, bestSellOrders = emptyMap())

        if (updated.isNotEmpty()) {
            logger.info("Updated ownership [{}], OpenSea order removed: [{}]", updated, openSeaOrder.id)
            ownershipRepository.save(updated)
        } else {
            logger.info("Deleted enriched ownership [{}], OpenSea order removed: [{}]", updated, openSeaOrder.id)
            ownershipRepository.delete(ownership.id)
        }

        val dto = ownershipService.enrichOwnership(updated)

        val event = OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = dto.id,
            ownership = dto
        )

        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}