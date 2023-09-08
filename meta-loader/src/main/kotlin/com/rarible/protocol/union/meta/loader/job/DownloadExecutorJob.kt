package com.rarible.protocol.union.meta.loader.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutor
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import java.time.Duration
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class DownloadExecutorJob(
    meterRegistry: MeterRegistry,
    workerName: String,
    private val executor: DownloadExecutor<*>,
    private val downloadTaskService: DownloadTaskService,
    private val pipeline: String,
    private val poolSize: Int
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = Duration.ofSeconds(1),
        errorDelay = Duration.ofSeconds(5),
    ),
    workerName = workerName
) {

    private val type = executor.type.lowercase()
    private val batches: Queue<List<Deferred<Unit>>> = LinkedBlockingQueue()
    private val inProgress = AtomicInteger()

    public override suspend fun handle() {
        // Initially we push to the executor x2 of its size to have buffer between next submit calls
        submitBatch(poolSize * 2)
        do {
            val start = System.currentTimeMillis()

            // Potentially here we can get memory leak in case if one of the tasks will be stuck,
            // but since we have timeout for all long operations, it should not happen
            batches.poll()?.awaitAll()?.let {
                logger.info(
                    "Handled {} {} ({}) download tasks ({}ms)",
                    it.size, type, pipeline, System.currentTimeMillis() - start
                )
            }
        } while (batches.isNotEmpty())

        delay(pollingPeriod.toMillis())
    }

    private suspend fun submitBatch(size: Int) {
        val tasks = downloadTaskService.getForExecution(
            type = type,
            pipeline = pipeline,
            limit = size
        ).map { it.toEvent() }.ifEmpty { return }

        inProgress.addAndGet(tasks.size)
        val batch = tasks.map { executor.submit(it, ::onTaskHandled) }
        batches.add(batch)
    }

    private suspend fun onTaskHandled(task: DownloadTaskEvent) {
        downloadTaskService.delete(task)
        val remains = inProgress.decrementAndGet()
        // if remain task count in pool is equal size of pool, we need to fill buffer again
        // such approach allow to utilize downloader pool effectively - we don't need to wait "slowest" task in batch
        if (remains == poolSize) {
            submitBatch(poolSize)
        }
    }
}
