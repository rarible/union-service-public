package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.core.UnionMetrics
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

// TODO move to MetaMetrics
@Component
class DownloadExecutorMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    fun onSuccessfulTask(
        type: String,
        blockchain: BlockchainDto,
        started: Instant,
        task: DownloadTaskEvent,
        retry: Int
    ) {
        onTaskHandled(started, blockchain, type, "ok", task, retry)
        onTaskDone(blockchain, type, "ok", task)
    }

    // Task debounced
    fun onSkippedTask(type: String, blockchain: BlockchainDto, started: Instant, task: DownloadTaskEvent, retry: Int) {
        onTaskHandled(started, blockchain, type, "skip", task, retry)
    }

    fun onForbiddenTask(
        type: String,
        blockchain: BlockchainDto,
        started: Instant,
        task: DownloadTaskEvent,
        retry: Int
    ) {
        onTaskHandled(started, blockchain, type, "forbidden", task, retry)
    }

    // Download failed, new status of the task is FAILED
    fun onFailedTask(type: String, blockchain: BlockchainDto, started: Instant, task: DownloadTaskEvent, retry: Int) {
        onTaskHandled(started, blockchain, type, "fail", task, retry)
        onTaskDone(blockchain, type, "fail", task)
    }

    // Download failed, but new status of task is RETRY
    fun onRetriedTask(type: String, blockchain: BlockchainDto, started: Instant, task: DownloadTaskEvent, retry: Int) {
        onTaskHandled(started, blockchain, type, "retry", task, retry)
    }

    // Delay between start date (i.e., date when related entity has been created) and date of first successful download
    fun onFirstSuccessfulDownload(
        type: String,
        blockchain: BlockchainDto,
        start: Instant,
        task: DownloadTaskEvent,
        retry: Int,
        status: SuccessfulDownloadStatus,
    ) {
        record(
            DOWNLOAD_DELAY,
            Duration.between(start, Instant.now()),
            PERCENTILES_99_95_75,
            tag(blockchain),
            type(type.lowercase()),
            tag("pipeline", task.pipeline.lowercase()),
            tag("retry", retry.toString()),
            tag("force", task.force.toString()),
            tag("status", status.name.lowercase())
        )
    }

    private fun onTaskHandled(
        started: Instant,
        blockchain: BlockchainDto,
        type: String,
        status: String,
        task: DownloadTaskEvent,
        retry: Int
    ) {
        record(
            DOWNLOAD_TASK,
            Duration.between(started, Instant.now()),
            tag(blockchain),
            type(type.lowercase()),
            status(status.lowercase()),
            tag("pipeline", task.pipeline.lowercase()),
            tag("retry", retry.toString()),
            tag("force", task.force.toString())
        )
    }

    private fun onTaskDone(
        blockchain: BlockchainDto,
        type: String,
        status: String,
        task: DownloadTaskEvent,
    ) {
        record(
            DOWNLOAD_TASK_TOTAL,
            Duration.between(task.scheduledAt, Instant.now()),
            tag(blockchain),
            type(type.lowercase()),
            status(status.lowercase()),
            tag("pipeline", task.pipeline.lowercase()),
            tag("force", task.force.toString())
        )
    }

    private companion object {

        const val DOWNLOAD_TASK = "download_task"
        const val DOWNLOAD_TASK_TOTAL = "download_task_total"
        const val DOWNLOAD_DELAY = "download_delay"
    }
}

enum class SuccessfulDownloadStatus {
    FULL,
    PARTIAL
}
