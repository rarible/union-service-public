package com.rarible.protocol.union.worker.job

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component

class BestOrderCheckJob(
    private val handler: BestOrderCheckJobHandler,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.priceUpdate.rate,
        errorDelay = properties.priceUpdate.rate
    ),
    workerName = "best-order-check-job"
) {

    override suspend fun handle() {
        handler.handle()
        delay(pollingPeriod)
    }
}

@Component
class BestOrderCheckJobHandler(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val collectionRepository: CollectionRepository,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService,
    blockchains: List<BlockchainDto>,
    properties: WorkerProperties,
) : JobHandler {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val updateRate = properties.priceUpdate.rate
    private val enabledBlockchains = blockchains.toSet()

    override suspend fun handle() {
        logger.info("BestOrderCheckJob started for blockchains: {}", enabledBlockchains)
        val notUpdatedSince = nowMillis() - updateRate

        logger.info("Checking Items with multi-currency orders updated after {}", notUpdatedSince)

        val itemsUpdated = itemRepository.findWithMultiCurrency(notUpdatedSince)
            .filter { enabledBlockchains.contains(it.blockchain) }
            .map {
                withIgnoredOptimisticLock {
                    enrichmentItemEventService.recalculateBestOrders(it, offchainEventMark("enrichment-in"))
                }
            }.sum()

        logger.info("Recalculated best Orders for {} Items", itemsUpdated)

        logger.info("Checking Collections with multi-currency orders updated after {}", notUpdatedSince)
        val collectionsUpdated = collectionRepository.findWithMultiCurrency(notUpdatedSince)
            .filter { enabledBlockchains.contains(it.blockchain) }
            .map {
                withIgnoredOptimisticLock {
                    enrichmentCollectionEventService.recalculateBestOrders(it, offchainEventMark("enrichment-in"))
                }
            }.sum()

        logger.info("Recalculated best Orders for {} Collections", collectionsUpdated)

        logger.info("Checking Ownerships with multi-currency orders updated after {}", notUpdatedSince)
        val ownershipsUpdated = ownershipRepository.findWithMultiCurrency(notUpdatedSince)
            .filter { enabledBlockchains.contains(it.blockchain) }
            .map {
                withIgnoredOptimisticLock {
                    enrichmentOwnershipEventService.recalculateBestOrders(it, offchainEventMark("enrichment-in"))
                }
            }.sum()

        logger.info("Recalculated best Orders for {} Ownerships", ownershipsUpdated)
    }

    private suspend fun withIgnoredOptimisticLock(call: suspend () -> Boolean): Int {
        return try {
            val updated = call()
            if (updated) 1 else 0
        } catch (ex: OptimisticLockingFailureException) {
            // ignoring this exception - if entity was updated by somebody during job,
            // it means item/ownership already actualized, and we don't need to recalculate it
            0
        }
    }

    private suspend fun Flow<Int>.sum(): Int {
        return this.fold(0) { a, b -> a + b }
    }
}
