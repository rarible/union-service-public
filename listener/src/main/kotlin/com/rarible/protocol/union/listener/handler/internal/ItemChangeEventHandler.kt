package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.enrichment.meta.item.ItemChangeListener
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import org.springframework.stereotype.Component

@Component
class ItemChangeEventHandler(
    private val listeners: List<ItemChangeListener>
) : InternalEventHandler<ItemChangeEvent> {

    override suspend fun handle(event: ItemChangeEvent) {
        listeners.forEach { listener -> listener.onItemChange(event) }
    }
}
