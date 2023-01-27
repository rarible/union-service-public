package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.OrderEventDto
import org.springframework.stereotype.Component

@Component
class OutgoingOrderEventMetricListener : OutgoingEventMetricListener<OrderEventDto>() {

    override suspend fun onEvent(event: OrderEventDto) = onEvent(
        event.orderId.blockchain,
        EventType.ORDER,
        event.eventTimeMarks
    )

}