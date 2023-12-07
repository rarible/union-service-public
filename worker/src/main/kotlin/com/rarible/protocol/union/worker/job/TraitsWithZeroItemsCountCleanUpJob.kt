package com.rarible.protocol.union.worker.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.service.TraitService
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import java.time.Duration

class TraitsWithZeroItemsCountCleanUpJob(
    meterRegistry: MeterRegistry,
    properties: WorkerProperties,
    val traitService: TraitService
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.traitsWithZeroItemsCountCleanUp.rate,
        errorDelay = Duration.ofHours(1),
    ),
    workerName = "traint_with_zero_items_count_cleanup_job"
) {

    private val enabled = properties.traitsWithZeroItemsCountCleanUp.enabled

    public override suspend fun handle() {
        if (!enabled) return
        traitService.deleteWithZeroItemsCount()
        delay(pollingPeriod)
    }
}
