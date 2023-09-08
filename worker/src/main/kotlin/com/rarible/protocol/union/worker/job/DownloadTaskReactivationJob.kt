package com.rarible.protocol.union.worker.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DownloadTaskReactivationJob(
    private val downloadTaskService: DownloadTaskService,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = Duration.ofMinutes(1),
        errorDelay = Duration.ofMinutes(1),
    ),
    workerName = "download_task_reactivation_job"
) {

    override suspend fun handle() {
        val result = downloadTaskService.reactivateStuckTasks()
        logger.info("Reactivated {} stuck download tasks", result)
        delay(pollingPeriod)
    }
}
