package com.rarible.protocol.union.worker.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

class MetaRefreshRequestCleanupJob(
    private val metaRefreshRequestRepository: MetaRefreshRequestRepository,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.collectionMetaRefreshRequestCleanup.rate,
        errorDelay = properties.collectionMetaRefreshRequestCleanup.rate
    ),
    workerName = "meta_refresh_request_cleanup_job"
) {

    override suspend fun handle() {
        metaRefreshRequestRepository.deleteCreatedBefore(Instant.now().minus(1, ChronoUnit.DAYS))
        delay(pollingPeriod)
    }
}

