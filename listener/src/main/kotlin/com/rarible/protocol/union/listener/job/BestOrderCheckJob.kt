package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BestOrderCheckJob(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    properties: UnionListenerProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val updateRate = properties.priceUpdate.rate

    @Scheduled(
        fixedRateString = "\${listener.price-update.rate}",
        initialDelayString = "\${listener.price-update.delay}"
    )
    fun updateBestOrderPrice() = runBlocking<Unit> {
        logger.info("BestOrderCheckJob started")
        val notUpdatedSince = nowMillis() - updateRate

        logger.info("Checking Items with multi-currency orders updated after {}", notUpdatedSince)

        val itemsUpdated = itemRepository.findWithMultiCurrency(notUpdatedSince).map { shortItem ->
            val updated = withIgnoredOptimisticLock {
                enrichmentItemEventService.recalculateBestOrders(shortItem)
            }
            if (updated) 1 else 0
        }.fold(0) { a, b -> a + b }

        logger.info("Recalculated best Orders for {} Items", itemsUpdated)

        logger.info("Checking Ownerships with multi-currency orders updated after {}", notUpdatedSince)
        val ownershipsUpdated = ownershipRepository.findWithMultiCurrency(notUpdatedSince).map { shortOwnership ->
            val updated = withIgnoredOptimisticLock {
                enrichmentOwnershipEventService.recalculateBestOrder(shortOwnership)
            }
            if (updated) 1 else 0
        }.fold(0) { a, b -> a + b }

        logger.info("Recalculated best Order for {} Ownerships", ownershipsUpdated)
    }

    private suspend fun withIgnoredOptimisticLock(call: suspend () -> Boolean): Boolean {
        return try {
            call()
        } catch (ex: OptimisticLockingFailureException) {
            // ignoring this exception - if entity was updated by somebody during job,
            // it means item/ownership already actualized, and we don't need to recalculate it
            false
        }
    }
}
