package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class DownloadExecutorMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    fun onSuccessfulTask(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(blockchain, type, "ok", pipeline, force)
    }

    // Task debounced
    fun onSkippedTask(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(blockchain, type, "skip", pipeline, force)
    }

    // Download failed, new status of the task is FAILED
    fun onFailedTask(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(blockchain, type, "fail", pipeline, force)
    }

    // Download failed, but new status of task is RETRY
    fun onRetriedTask(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(blockchain, type, "retry", pipeline, force)
    }

    private fun onTaskHandled(
        blockchain: BlockchainDto,
        type: String,
        status: String,
        pipeline: String,
        force: Boolean
    ) {
        increment(
            DOWNLOAD_TASK,
            tag(blockchain),
            type(type.lowercase()),
            status(status.lowercase()),
            tag("pipeline", pipeline.lowercase()),
            tag("force", force.toString())
        )
    }

    private companion object {

        const val DOWNLOAD_TASK = "download_task"
    }
}