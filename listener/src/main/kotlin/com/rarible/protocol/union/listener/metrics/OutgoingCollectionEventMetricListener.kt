package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.CollectionEventDto
import org.springframework.stereotype.Component

@Component
class OutgoingCollectionEventMetricListener : OutgoingEventMetricListener<CollectionEventDto>() {

    override suspend fun onEvent(event: CollectionEventDto) = onEvent(
        event.collectionId.blockchain,
        EventType.COLLECTION,
        event.eventTimeMarks
    )

}