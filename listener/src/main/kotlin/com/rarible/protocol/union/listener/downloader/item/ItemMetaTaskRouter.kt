package com.rarible.protocol.union.listener.downloader.item

import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.listener.downloader.DownloadTaskRouter
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskRouter : DownloadTaskRouter {

    override suspend fun send(tasks: List<DownloadTask>, pipeline: String) {
        // TODO PT-49
    }
}