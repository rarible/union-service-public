package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.enrichment.event.CollectionEventListener
import com.rarible.protocol.union.enrichment.event.CollectionEventUpdate
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionEventService(
    private val collectionEventListeners: List<CollectionEventListener>
) {

    suspend fun onCollectionUpdated(collection: CollectionDto) {
        notifyUpdate(collection)
    }

    private suspend fun notifyUpdate(collection: CollectionDto) = coroutineScope {
        val event = CollectionEventUpdate(collection)
        collectionEventListeners.forEach { it.onEvent(event) }
    }

}
