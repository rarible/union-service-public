package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class DownloadExecutorMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    fun onSuccessfulTask(started: Instant, blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(started, blockchain, type, "ok", pipeline, force)
    }

    // Task debounced
    fun onSkippedTask(started: Instant, blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(started, blockchain, type, "skip", pipeline, force)
    }

    // Download failed, new status of the task is FAILED
    fun onFailedTask(started: Instant, blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(started, blockchain, type, "fail", pipeline, force)
    }

    // Download failed, but new status of task is RETRY
    fun onRetriedTask(started: Instant, blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(started, blockchain, type, "retry", pipeline, force)
    }

    private fun onTaskHandled(
        started: Instant,
        blockchain: BlockchainDto,
        type: String,
        status: String,
        pipeline: String,
        force: Boolean
    ) {
        meterRegistry.timer(
            DOWNLOAD_TASK,
            listOf(
                tag(blockchain),
                type(type.lowercase()),
                status(status.lowercase()),
                tag("pipeline", pipeline.lowercase()),
                tag("force", force.toString())
            )
        ).record(Duration.between(started, Instant.now()))
    }

    private companion object {

        const val DOWNLOAD_TASK = "download_task"
    }
}