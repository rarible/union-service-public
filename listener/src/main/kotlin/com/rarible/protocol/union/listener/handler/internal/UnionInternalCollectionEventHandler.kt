package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionSetBaseUriEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionEventService
import com.rarible.protocol.union.enrichment.service.ReconciliationEventService
import org.springframework.stereotype.Component

@Component
class UnionInternalCollectionEventHandler(
    private val collectionEventService: EnrichmentCollectionEventService,
    private val reconciliationEventService: ReconciliationEventService
) {

    suspend fun onEvent(event: UnionCollectionEvent) {
        try {
            when (event) {
                is UnionCollectionUpdateEvent -> collectionEventService.onCollectionUpdate(event)
                is UnionCollectionChangeEvent -> collectionEventService.onCollectionChanged(event)
                is UnionCollectionSetBaseUriEvent -> collectionEventService.onCollectionSetBaseUri(event)
            }
        } catch (e: Throwable) {
            reconciliationEventService.onCorruptedCollection(event.collectionId)
            throw e
        }
    }
}
