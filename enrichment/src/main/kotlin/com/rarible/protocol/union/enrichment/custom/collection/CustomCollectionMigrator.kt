package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomCollectionMigrator(
    private val eventProducer: UnionInternalBlockchainEventProducer,
    private val updaters: List<CustomCollectionUpdater>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Here we need just to send events, collections will be replaced in listener
    suspend fun migrate(items: List<UnionItem>) = coroutineScope {
        if (items.isEmpty()) {
            return@coroutineScope
        }
        // TODO ideally, there should be check - if collection already substituted, but it is possible
        // only if we have items in Union
        items.map { item ->
            async {
                val itemId = item.id
                val eventTimeMarks = offchainEventMark("enrichment-in")
                val message = KafkaEventFactory.internalItemEvent(UnionItemChangeEvent(itemId, eventTimeMarks))
                eventProducer.getProducer(itemId.blockchain).send(message)
                updaters.map { async { it.update(item) } }.awaitAll()
            }
        }.awaitAll()

        logger.info("Custom collection recalculated for Items: {}", items.map { it.id.fullId() })
    }

}