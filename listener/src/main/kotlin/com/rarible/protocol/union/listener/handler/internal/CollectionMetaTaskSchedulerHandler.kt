package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.listener.downloader.CollectionMetaTaskScheduler
import org.springframework.stereotype.Component

@Component
class CollectionMetaTaskSchedulerHandler(
    private val scheduler: CollectionMetaTaskScheduler
) : InternalBatchEventHandler<DownloadTask> {

    override suspend fun handle(events: List<DownloadTask>) {
        scheduler.schedule(events)
    }

}