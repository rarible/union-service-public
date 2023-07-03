package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.core.model.download.DownloadTask
import org.slf4j.LoggerFactory

class DownloadExecutorHandler(
    private val pipeline: String,
    downloadExecutorManager: DownloadExecutorManager
) : RaribleKafkaBatchEventHandler<DownloadTask> {

    private val executor = downloadExecutorManager.getExecutor(pipeline)

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<DownloadTask>) {
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