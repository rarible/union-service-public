package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import org.springframework.stereotype.Component

@Component
class ItemMetaNotifier : DownloadNotifier<UnionMeta> {

    override suspend fun notify(entry: DownloadEntry<UnionMeta>) {
        // TODO PT-49
    }
}