package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.ReconciliationEventService
import org.springframework.stereotype.Component

@Component
class UnionInternalItemEventHandler(
    private val itemEventService: EnrichmentItemEventService,
    private val reconciliationEventService: ReconciliationEventService
) {

    @CaptureTransaction("UnionItemEvent")
    suspend fun onEvent(event: UnionItemEvent) {
        try {
            when (event) {
                is UnionItemUpdateEvent -> {
                    itemEventService.onItemUpdated(event)
                }
                is UnionItemDeleteEvent -> {
                    itemEventService.onItemDeleted(event)
                }
                is UnionItemChangeEvent -> {
                    itemEventService.onItemChanged(event)
                }
            }
        } catch (e: Throwable) {
            reconciliationEventService.onCorruptedItem(event.itemId)
            throw e
        }
    }
}
