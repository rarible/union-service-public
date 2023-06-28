package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.core.model.download.DownloadTask

class DownloadExecutorHandler(
    pipeline: String,
    downloadExecutorManager: DownloadExecutorManager
) : RaribleKafkaBatchEventHandler<DownloadTask> {

    private val executor = downloadExecutorManager.getExecutor(pipeline)

    override suspend fun handle(event: List<DownloadTask>) {
        executor.execute(event)
    }
}