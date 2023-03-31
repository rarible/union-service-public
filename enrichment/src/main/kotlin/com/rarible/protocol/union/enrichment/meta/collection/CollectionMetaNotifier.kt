package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import org.springframework.stereotype.Component

@Component
class CollectionMetaNotifier(
    private val eventProducer: UnionInternalBlockchainEventProducer
) : DownloadNotifier<UnionCollectionMeta> {

    override suspend fun notify(entry: DownloadEntry<UnionCollectionMeta>) {
        val itemId = IdParser.parseCollectionId(entry.id)
        val eventTimeMarks = offchainEventMark("enrichment-in")
        val message = KafkaEventFactory.internalCollectionEvent(UnionCollectionChangeEvent(itemId, eventTimeMarks))
        eventProducer.getProducer(itemId.blockchain).send(message).ensureSuccess()
    }
}