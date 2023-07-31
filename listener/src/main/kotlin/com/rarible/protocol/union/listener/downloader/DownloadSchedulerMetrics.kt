package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class DownloadSchedulerMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    /**
     * Successfully scheduled task
     */
    fun onScheduledTask(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(blockchain, type, "ok", pipeline, force)
    }

    /**
     * Task already scheduled previously, so skipped
     */
    fun onSkippedTask(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        onTaskHandled(blockchain, type, "skip", pipeline, force)
    }

    private fun onTaskHandled(
        blockchain: BlockchainDto,
        type: String,
        status: String,
        pipeline: String,
        force: Boolean
    ) {
        increment(
            DOWNLOAD_SCHEDULE,
            tag(blockchain),
            type(type.lowercase()),
            status(status.lowercase()),
            tag("pipeline", pipeline.lowercase()),
            tag("force", force.toString())
        )
    }

    private companion object {

        const val DOWNLOAD_SCHEDULE = "download_schedule"
    }
}
