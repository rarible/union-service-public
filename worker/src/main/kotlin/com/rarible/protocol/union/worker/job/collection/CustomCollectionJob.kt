package com.rarible.protocol.union.worker.job.collection

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
class CustomCollectionJob(
    private val eventProducer: UnionInternalBlockchainEventProducer,
    private val customCollectionItemFetcherProvider: CustomCollectionItemFetcherProvider,
    private val updaters: List<CustomCollectionUpdater>
) {

    private val batchSize = 50

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * @param name identifier of rule set in the configuration
     * @param continuation last state of the migration (format depends on the rule type)
     */
    suspend fun migrate(name: String, continuation: String?): String? {
        val fetchers = customCollectionItemFetcherProvider.get(name)
        val state = continuation?.let { CustomCollectionJobState(it) }
        var currentFetcher = state?.rule ?: 0
        var currentState = state?.state
        while (currentFetcher < fetchers.size) {
            val fetcher = fetchers[currentFetcher]
            val next = fetcher.next(currentState, batchSize)
            if (next.state != null) {
                migrate(next.items)
                logger.info("Moving {} Items to custom collection: {}", next.items.size, name)
                return CustomCollectionJobState(currentFetcher, next.state).toString()
            }
            currentFetcher++
            currentState = null
        }
        return null
    }

    // Here we need just to send events, collections will be replaced in listener
    private suspend fun migrate(items: List<UnionItem>) = coroutineScope {
        // TODO ideally, there should be check - if collection already substituted, but it is possible
        // only if we have items in Union
        items.map { item ->
            async {
                val itemId = item.id
                val eventTimeMarks = offchainEventMark("enrichment-in")
                val message = KafkaEventFactory.internalItemEvent(UnionItemChangeEvent(itemId, eventTimeMarks))
                updaters.forEach { it.update(item) }
                eventProducer.getProducer(itemId.blockchain).send(message)
            }
        }.awaitAll()
    }
}


