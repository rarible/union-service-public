package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import java.time.Instant

class DownloadExecutorMetrics(
    meterRegistry: MeterRegistry,
    private val blockchainExtractor: (id: String) -> BlockchainDto,
    private val type: String,
) : UnionMetrics(
    meterRegistry
) {

    fun onSuccessfulTask(started: Instant, task: DownloadTask) {
        val blockchain = blockchainExtractor(task.id)
        onTaskHandled(started, blockchain, type, "ok", task)
        onTaskDone(blockchain, type, "ok", task)
    }

    // Task debounced
    fun onSkippedTask(started: Instant, task: DownloadTask) {
        val blockchain = blockchainExtractor(task.id)
        onTaskHandled(started, blockchain, type, "skip", task)
    }

    // Download failed, new status of the task is FAILED
    fun onFailedTask(started: Instant, task: DownloadTask) {
        val blockchain = blockchainExtractor(task.id)
        onTaskHandled(started, blockchain, type, "fail", task)
        onTaskDone(blockchain, type, "fail", task)
    }

    // Download failed, but new status of task is RETRY
    fun onRetriedTask(started: Instant, task: DownloadTask) {
        val blockchain = blockchainExtractor(task.id)
        onTaskHandled(started, blockchain, type, "retry", task)
    }

    private fun onTaskHandled(
        started: Instant,
        blockchain: BlockchainDto,
        type: String,
        status: String,
        task: DownloadTask,
    ) {
        meterRegistry.timer(
            DOWNLOAD_TASK,
            listOf(
                tag(blockchain),
                type(type.lowercase()),
                status(status.lowercase()),
                tag("pipeline", task.pipeline.lowercase()),
                tag("force", task.force.toString())
            )
        ).record(Duration.between(started, Instant.now()))
    }

    private fun onTaskDone(
        blockchain: BlockchainDto,
        type: String,
        status: String,
        task: DownloadTask,
    ) {
        meterRegistry.timer(
            DOWNLOAD_TASK_TOTAL,
            listOf(
                tag(blockchain),
                type(type.lowercase()),
                status(status.lowercase()),
                tag("pipeline", task.pipeline.lowercase()),
                tag("force", task.force.toString())
            )
        ).record(Duration.between(task.scheduledAt, Instant.now()))
    }

    private companion object {

        const val DOWNLOAD_TASK = "download_task"
        const val DOWNLOAD_TASK_TOTAL = "download_task_total"
    }
}