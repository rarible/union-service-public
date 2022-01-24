package com.rarible.protocol.union.listener.handler.wrapped

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import org.springframework.stereotype.Component

@Component
class UnionWrappedItemEventHandler(
    private val itemEventService: EnrichmentItemEventService
) {

    @CaptureTransaction("UnionItemEvent")
    suspend fun onEvent(event: UnionItemEvent) {
        when (event) {
            is UnionItemUpdateEvent -> {
                itemEventService.onItemUpdated(event.item)
            }
            is UnionItemDeleteEvent -> {
                itemEventService.onItemDeleted(event.itemId)
            }
        }
    }
}