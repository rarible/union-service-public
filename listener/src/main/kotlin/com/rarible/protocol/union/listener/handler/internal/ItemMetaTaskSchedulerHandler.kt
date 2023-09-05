package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.listener.downloader.ItemMetaTaskScheduler
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskSchedulerHandler(
    private val scheduler: ItemMetaTaskScheduler
) : InternalBatchEventHandler<DownloadTaskEvent> {

    override suspend fun handle(events: List<DownloadTaskEvent>) {
        scheduler.schedule(events)
    }
}
