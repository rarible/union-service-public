package com.rarible.protocol.union.worker.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaRefreshService
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshState
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshStatus
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.delay
import java.time.Instant

class MetaAutoRefreshJob(
    private val metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository,
    private val itemMetaRefreshService: ItemMetaRefreshService,
    private val esActivityRepository: EsActivityRepository,
    private val simpleHashEnabled: Boolean,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.metaAutoRefresh.rate,
        errorDelay = properties.metaAutoRefresh.errorDelay,
    ),
    workerName = "meta_auto_refresh_job"
) {
    private val createdPeriod = properties.metaAutoRefresh.createdPeriod
    private val refreshedPeriod = properties.metaAutoRefresh.refreshedPeriod
    private val numberOfCollectionsToCheck = properties.metaAutoRefresh.numberOfCollectionsToCheck
    private val rate = properties.metaAutoRefresh.rate.toMillis()

    public override suspend fun handle() {
        logger.info("Starting MetaAutoRefreshJob")
        loadTradableCollections().forEach { collectionId ->
            itemMetaRefreshService.runRefreshIfAllowed(
                collectionId = collectionId,
                full = true,
                withSimpleHash = simpleHashEnabled,
                priority = MetaRefreshRequest.Priority.PRIORITY_HIGH
            )
        }
        metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minus(createdPeriod)).collect {
            processState(it)
        }
        metaAutoRefreshStateRepository.loadToCheckRefreshed(
            createFromDate = Instant.now().minus(createdPeriod),
            refreshedFromDate = Instant.now().minus(refreshedPeriod)
        ).collect {
            processState(it)
        }
        logger.info("Finished MetaAutoRefreshJob")
        delay(rate)
    }

    private suspend fun loadTradableCollections(): List<CollectionIdDto> {
        if (numberOfCollectionsToCheck == 0) {
            return emptyList()
        }
        val since = Instant.now().minusMillis(rate)
        return BlockchainDto.values()
            .flatMap { blockchain ->
                esActivityRepository.findTradedDistinctCollections(blockchain, since, numberOfCollectionsToCheck)
            }
    }

    private suspend fun processState(state: MetaAutoRefreshState) {
        val collectionId = IdParser.parseCollectionId(state.id)
        val scheduled = itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId, simpleHashEnabled)
        if (scheduled) {
            LogUtils.addToMdc(IdParser.parseCollectionId(state.id)) {
                logger.info("Scheduled auto refresh for collection: ${state.id}")
            }
            metaAutoRefreshStateRepository.save(
                state.copy(
                    status = MetaAutoRefreshStatus.REFRESHED,
                    lastRefreshedAt = Instant.now(),
                )
            )
        }
    }
}
