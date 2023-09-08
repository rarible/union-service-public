package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.service.DownloadTaskService

class MongoMetaTaskRouter(
    private val type: String,
    private val downloadTaskService: DownloadTaskService
) : DownloadTaskRouter {

    override suspend fun send(tasks: List<DownloadTaskEvent>, pipeline: String) {
        downloadTaskService.update(type, tasks)
    }
}
