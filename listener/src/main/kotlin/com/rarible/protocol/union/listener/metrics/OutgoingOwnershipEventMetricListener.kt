package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.springframework.stereotype.Component

@Component
class OutgoingOwnershipEventMetricListener : OutgoingEventMetricListener<OwnershipEventDto>() {

    override suspend fun onEvent(event: OwnershipEventDto) = onEvent(
        event.ownershipId.blockchain,
        EventType.OWNERSHIP,
        event.eventTimeMarks
    )

}