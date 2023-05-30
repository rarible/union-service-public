package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import org.springframework.stereotype.Component

@Component
class ItemMetaNotifier(
    private val eventProducer: UnionInternalItemEventProducer
) : DownloadNotifier<UnionMeta> {

    override suspend fun notify(entry: DownloadEntry<UnionMeta>) {
        eventProducer.sendChangeEvent(IdParser.parseItemId(entry.id))
    }
}