package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.enrichment.meta.downloader.DownloadTaskPublisher
import com.rarible.protocol.union.enrichment.meta.downloader.model.DownloadTask
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskPublisher : DownloadTaskPublisher {

    override suspend fun publish(tasks: List<DownloadTask>) {
        // TODO PT-49
    }
}