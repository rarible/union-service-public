package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.model.download.DownloadTask

class DownloadExecutorHandler(
    pipeline: String,
    downloadExecutorManager: DownloadExecutorManager
) : ConsumerBatchEventHandler<DownloadTask> {

    private val executor = downloadExecutorManager.getExecutor(pipeline)

    override suspend fun handle(event: List<DownloadTask>) {
        executor.execute(event)
    }

}