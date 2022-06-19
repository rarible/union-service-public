package com.rarible.protocol.union.listener.job

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

class ReconciliationMarkJob(
    private val handler: ReconciliationMarkJobHandler,
    properties: UnionListenerProperties,
    meterRegistry: MeterRegistry,
): SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.reconcileMarks.rate,
        errorDelay = properties.reconcileMarks.rate
    ),
    workerName = "reconciliation-mark-job"
) {
    override suspend fun handle() {
        handler.handle()
        delay(pollingPeriod)
    }
}

@Component
class ReconciliationMarkJobHandler(
    private val reconciliationMarkRepository: ReconciliationMarkRepository,
    private val refreshService: EnrichmentRefreshService,
    activeBlockchains: List<BlockchainDto>
) : JobHandler {

    private val batch: Int = 20
    private val blockchains = activeBlockchains.toSet()

    private val logger = LoggerFactory.getLogger(javaClass)

    private val types = listOf(
        ReconciliationMarkType.ITEM,
        ReconciliationMarkType.OWNERSHIP,
        ReconciliationMarkType.COLLECTION
    )

    override suspend fun handle() {
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
        try {
            when (type) {
                ReconciliationMarkType.ITEM -> {
                    val itemId = IdParser.parseItemId(markId)
                    if (blockchains.contains(itemId.blockchain)) {
                        refreshService.reconcileItem(itemId, false)
                    }
                }
                ReconciliationMarkType.OWNERSHIP -> {
                    val ownershipId = OwnershipIdParser.parseFull(markId)
                    if (blockchains.contains(ownershipId.blockchain)) {
                        refreshService.reconcileOwnership(ownershipId)
                    }
                }
                ReconciliationMarkType.COLLECTION -> {
                    val collectionId = IdParser.parseCollectionId(markId)
                    if(blockchains.contains(collectionId.blockchain)) {
                        refreshService.reconcileCollection(collectionId)
                    }
                }
            }
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.info("Unable to reconcile mark [{}], NOT_FOUND received: {}", markId, e.data)
            } else {
                throw e
            }
        } catch (e: UnionNotFoundException) {
            logger.info("Unable to reconcile mark [{}], NOT_FOUND received: {}", markId, e.message)
        }
    }
}