package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.listener.downloader.CollectionMetaTaskScheduler
import org.springframework.stereotype.Component

@Component
class CollectionMetaTaskSchedulerHandler(
    private val scheduler: CollectionMetaTaskScheduler
) : InternalBatchEventHandler<DownloadTaskEvent> {

    override suspend fun handle(events: List<DownloadTaskEvent>) {
        scheduler.schedule(events)
    }
}
