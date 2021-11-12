package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.event.OutgoingCollectionEventListener
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.CollectionEventDto
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "app", subtype = "event")
class UnionCollectionEventHandler(
    private val collectionEventListeners: List<OutgoingCollectionEventListener>
) : IncomingEventHandler<CollectionEventDto> {

    override suspend fun onEvent(event: CollectionEventDto) {
        collectionEventListeners.onEach { it.onEvent(event) }
    }
}

