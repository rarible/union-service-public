package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.listener.service.EnrichmentCollectionEventService
import org.springframework.stereotype.Component

@Component
class UnionWrappedCollectionEventHandler(
    private val collectionEventService: EnrichmentCollectionEventService,
    private val reconciliationEventService: ReconciliationEventService
) {
    @CaptureTransaction("UnionCollectionEvent")
    suspend fun onEvent(event: UnionCollectionEvent) {
        try {
            if (event is UnionCollectionUpdateEvent) {
                collectionEventService.onCollectionUpdate(event.collection)
            }
        } catch (e: Throwable) {
            reconciliationEventService.onCorruptedCollection(event.collectionId)
            throw e
        }
    }
}