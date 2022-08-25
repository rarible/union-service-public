package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import org.springframework.stereotype.Component

@Component
class ItemMetaNotifier(
    private val eventProducer: UnionInternalBlockchainEventProducer
) : DownloadNotifier<UnionMeta> {

    override suspend fun notify(entry: DownloadEntry<UnionMeta>) {
        val itemId = IdParser.parseItemId(entry.id)
        val message = KafkaEventFactory.internalItemEvent(UnionItemChangeEvent(itemId))
        eventProducer.getProducer(itemId.blockchain).send(message).ensureSuccess()
    }
}