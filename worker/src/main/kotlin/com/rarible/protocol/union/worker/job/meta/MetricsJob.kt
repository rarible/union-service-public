package com.rarible.protocol.union.worker.job.meta

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.meta.MetaMetrics
import com.rarible.protocol.union.enrichment.repository.DownloadTaskRepository
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MetricsJob(
    meterRegistry: MeterRegistry,
    private val markers: List<MetricsMarker>
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        // 1m is default polling period for Prom
        pollingPeriod = Duration.ofMinutes(1),
        errorDelay = Duration.ofMinutes(1),
    ),
    workerName = "metrics_job"
) {
    override suspend fun handle() {
        val start = System.currentTimeMillis()
        markers.forEach { it.mark() }
        val spent = System.currentTimeMillis() - start
        val delay = pollingPeriod.minusMillis(System.currentTimeMillis() - start)
        if (delay.isNegative) {
            logger.warn("Job {} takes too much time for execution: {}ms", workerName, spent)
            // It's better to slowdown metrics gathering instead of load worker continuously
            delay(pollingPeriod)
            return
        }
        delay(delay)
    }
}

interface MetricsMarker {
    suspend fun mark()
}

@Component
class MetaDownloadQueueMetricsMarker(
    private val metrics: List<MetaMetrics>,
    private val downloadTaskRepository: DownloadTaskRepository
) : MetricsMarker {

    // Let's set upper limit of trackable queue size - in order to do not boil Mongo
    private val limit = 1_000_000

    override suspend fun mark() {
        coroutineScope {
            metrics.map {
                it.pipelines.map { pipeline ->
                    async {
                        val queueSize = downloadTaskRepository.getTaskCountInPipeline(
                            type = it.type,
                            pipeline = pipeline,
                            limit = limit
                        )
                        it.onMetaQueueSizeUpdated(pipeline, queueSize)
                    }
                }
            }
        }.flatten().awaitAll()
    }
}
