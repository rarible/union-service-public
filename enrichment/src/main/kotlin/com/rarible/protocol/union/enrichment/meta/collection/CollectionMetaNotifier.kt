package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import org.springframework.stereotype.Component

@Component
class CollectionMetaNotifier(
    private val eventProducer: UnionInternalCollectionEventProducer
) : DownloadNotifier<UnionCollectionMeta> {

    override suspend fun notify(entry: DownloadEntry<UnionCollectionMeta>) {
        eventProducer.sendChangeEvent(IdParser.parseCollectionId(entry.id))
    }
}
