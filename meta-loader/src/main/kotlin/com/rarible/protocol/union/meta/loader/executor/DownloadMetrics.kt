package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class DownloadMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    fun onSuccessfulTask(blockchain: BlockchainDto, type: String, pipeline: String) {
        onTaskHandled(blockchain, type, "ok", pipeline)
    }

    // Task debounced
    fun onSkippedTask(blockchain: BlockchainDto, type: String, pipeline: String) {
        onTaskHandled(blockchain, type, "skip", pipeline)
    }

    // Download failed, new status of the task is FAILED
    fun onFailedTask(blockchain: BlockchainDto, type: String, pipeline: String) {
        onTaskHandled(blockchain, type, "fail", pipeline)
    }

    // Download failed, but new status of task is RETRY
    fun onRetriedTask(blockchain: BlockchainDto, type: String, pipeline: String) {
        onTaskHandled(blockchain, type, "retry", pipeline)
    }

    private fun onTaskHandled(blockchain: BlockchainDto, type: String, status: String, pipeline: String) {
        increment(
            DOWNLOAD_TASK_SKIPPED,
            tag(blockchain),
            tag("pipeline", pipeline),
            type(type.lowercase()),
            status(status.lowercase())
        )
    }

    private companion object {

        const val DOWNLOAD_TASK_SKIPPED = "download_task"
    }
}