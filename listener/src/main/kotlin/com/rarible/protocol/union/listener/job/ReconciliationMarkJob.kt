package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkType
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReconciliationMarkJob(
    private val reconciliationMarkRepository: ReconciliationMarkRepository,
    private val refreshService: EnrichmentRefreshService
) {

    private val batch: Int = 20

    private val logger = LoggerFactory.getLogger(javaClass)

    private val types = listOf(
        ReconciliationMarkType.ITEM,
        ReconciliationMarkType.OWNERSHIP,
    )

    @Scheduled(
        fixedRateString = "\${listener.reconcile-marks.rate}",
        initialDelayString = "\${listener.reconcile-marks.delay}"
    )
    fun reconcileMarkedRecords() = runBlocking {
        types.forEach { type ->
            var reconciledEntities = 0
            logger.info("Starting to reconcile marks for {}", type)
            do {
                val reconciled = reconcileEntities(type)
                reconciledEntities += reconciled
            } while (reconciled > 0)
            logger.info("Finished to reconcile marks for {}, {} entities has been reconciled", type, reconciledEntities)
        }
    }

    private suspend fun reconcileEntities(type: ReconciliationMarkType): Int {
        val marks = reconciliationMarkRepository.findByType(type, batch)
        if (marks.isEmpty()) {
            return 0
        }
        logger.info("Found {} {} reconciliation marks", type, marks.size)
        var withFails = 0

        marks.forEach {
            try {
                reconcileEntity(it.id, type)
                reconciliationMarkRepository.delete(it)
            } catch (e: Exception) {
                withFails++
                reconciliationMarkRepository.save(
                    it.copy(retries = it.retries + 1, lastUpdatedAt = nowMillis())
                )
                logger.warn("Unable to reconcile {} [{}]:", type, it.id, e)
            }
        }
        // means "hasMore", but if there were a lot of fails during updates, it's better to stop current
        // job iteration in order to prevent endless spam of errors
        return if (withFails > marks.size / 2) 0 else marks.size
    }

    private suspend fun reconcileEntity(markId: String, type: ReconciliationMarkType) {
        when (type) {
            ReconciliationMarkType.ITEM -> {
                val itemId = IdParser.parseItemId(markId)
                refreshService.reconcileItem(itemId, false)
            }
            ReconciliationMarkType.OWNERSHIP -> {
                val ownershipId = OwnershipIdParser.parseFull(markId)
                refreshService.reconcileOwnership(ownershipId)
            }
        }
    }
}