package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.enrichment.repository.ItemReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReconciliationMarkJob(
    private val itemReconciliationMarkRepository: ItemReconciliationMarkRepository,
    private val ownershipReconciliationMarkRepository: OwnershipReconciliationMarkRepository,
    private val refreshService: EnrichmentRefreshService
) {

    private val batch: Int = 20

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedRateString = "\${listener.reconcile-marks.rate}",
        initialDelayString = "\${listener.reconcile-marks.delay}"
    )
    fun reconcileMarkedRecords() = runBlocking<Unit> {
        var reconciledItems = 0
        logger.info("Starting to reconcile marked Items")
        do {
            val reconciled = reconcileItems()
            reconciledItems += reconciled
        } while (reconciled > 0)

        var reconciledOwnerships = 0
        logger.info("Starting to reconcile marked Ownerships")
        do {
            val reconciled = reconcileOwnerships()
            reconciledOwnerships += reconciled
        } while (reconciled > 0)

        logger.info(
            "Finished to reconcile marked records, {} Items and {} has been reconciled",
            reconciledItems,
            reconciledOwnerships
        )
    }

    private suspend fun reconcileItems(): Int {
        val items = itemReconciliationMarkRepository.findAll(batch)
        if (items.isEmpty()) {
            return 0
        }
        logger.info("Found {} Item reconciliation marks", items.size)
        items.forEach {
            refreshService.reconcileItem(it.id.toDto(), false)
            itemReconciliationMarkRepository.delete(it)
        }
        // means "hasMore"
        return items.size
    }

    private suspend fun reconcileOwnerships(): Int {
        val ownerships = ownershipReconciliationMarkRepository.findAll(batch)
        if (ownerships.isEmpty()) {
            return 0
        }
        logger.info("Found {} Ownership reconciliation marks", ownerships.size)
        ownerships.forEach {
            refreshService.refreshOwnership(it.id.toDto())
            ownershipReconciliationMarkRepository.delete(it)
        }
        // means "hasMore"
        return ownerships.size
    }

}