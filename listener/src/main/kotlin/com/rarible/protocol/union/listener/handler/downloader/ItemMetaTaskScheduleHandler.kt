package com.rarible.protocol.union.listener.handler.downloader

import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.listener.downloader.item.ItemMetaTaskScheduler
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduleHandler(
    private val scheduler: ItemMetaTaskScheduler
) : InternalBatchEventHandler<DownloadTask> {

    override suspend fun handle(events: List<DownloadTask>) {
        scheduler.schedule(events)
    }

}