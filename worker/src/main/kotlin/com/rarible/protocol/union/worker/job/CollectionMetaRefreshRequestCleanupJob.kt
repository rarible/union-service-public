package com.rarible.protocol.union.worker.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRefreshRequestRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

class CollectionMetaRefreshRequestCleanupJob(
    private val collectionMetaRefreshRequestRepository: CollectionMetaRefreshRequestRepository,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.collectionMetaRefreshRequestCleanup.rate,
        errorDelay = properties.collectionMetaRefreshRequestCleanup.rate
    ),
    workerName = "collection-meta-refresh-request-cleanup-job"
) {

    override suspend fun handle() {
        collectionMetaRefreshRequestRepository.deleteCreatedBefore(Instant.now().minus(1, ChronoUnit.DAYS))
        delay(pollingPeriod)
    }
}

