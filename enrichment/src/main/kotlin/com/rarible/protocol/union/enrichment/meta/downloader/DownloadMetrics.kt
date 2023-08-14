package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class DownloadMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(
    meterRegistry
) {

    /**
     * Counter for direct (sync) downloads. I.e., such downloads performed in API/Listener etc without
     * usage of pipeline mechanism.
     */
    fun onRequestSucceed(blockchain: BlockchainDto, type: String) {
        onRequestHandled(blockchain, type, "ok")
    }

    fun onRequestFailed(blockchain: BlockchainDto, type: String) {
        onRequestHandled(blockchain, type, "fail")
    }

    fun onTaskScheduled(blockchain: BlockchainDto, type: String, pipeline: String, force: Boolean) {
        increment(
            DOWNLOAD_TASK_SCHEDULED,
            tag(blockchain),
            type(type.lowercase()),
            tag("pipeline", pipeline.lowercase()),
            tag("force", force.toString()),
        )
    }

    private fun onRequestHandled(blockchain: BlockchainDto, type: String, status: String) {
        increment(
            DOWNLOAD_REQUEST,
            tag(blockchain),
            type(type.lowercase()),
            status(status.lowercase())
        )
    }

    private companion object {

        const val DOWNLOAD_REQUEST = "download_request"
        const val DOWNLOAD_TASK_SCHEDULED = "download_task_scheduled"
    }
}
