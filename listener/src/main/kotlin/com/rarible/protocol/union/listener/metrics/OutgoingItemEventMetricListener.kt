package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.ItemEventDto
import org.springframework.stereotype.Component

@Component
class OutgoingItemEventMetricListener : OutgoingEventMetricListener<ItemEventDto>() {

    override suspend fun onEvent(event: ItemEventDto) = onEvent(
        event.itemId.blockchain,
        EventType.ITEM,
        event.eventTimeMarks
    )

}