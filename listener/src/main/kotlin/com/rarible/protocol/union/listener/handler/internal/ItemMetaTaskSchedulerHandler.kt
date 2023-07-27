package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.listener.downloader.ItemMetaTaskScheduler
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskSchedulerHandler(
    private val scheduler: ItemMetaTaskScheduler
) : InternalBatchEventHandler<DownloadTask> {

    override suspend fun handle(events: List<DownloadTask>) {
        scheduler.schedule(events)
    }
}
