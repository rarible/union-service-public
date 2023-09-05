package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import org.slf4j.LoggerFactory

class DownloadExecutorHandler(
    private val pipeline: String,
    downloadExecutorManager: DownloadExecutorManager
) : RaribleKafkaBatchEventHandler<DownloadTaskEvent> {

    private val executor = downloadExecutorManager.getExecutor(pipeline)

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<DownloadTaskEvent>) {
        val start = System.currentTimeMillis()
        executor.execute(event)
        logger.info(
            "Handled {} events for pipeline {} for {} ({}ms)",
            event.size,
            pipeline,
            executor.type,
            System.currentTimeMillis() - start
        )
    }
}
