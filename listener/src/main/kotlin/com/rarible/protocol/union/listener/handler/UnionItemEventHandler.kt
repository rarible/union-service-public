package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "app", subtype = "event")
class UnionItemEventHandler(
    private val itemEventService: EnrichmentItemEventService
) : IncomingEventHandler<UnionItemEvent> {

    override suspend fun onEvent(event: UnionItemEvent) {
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
