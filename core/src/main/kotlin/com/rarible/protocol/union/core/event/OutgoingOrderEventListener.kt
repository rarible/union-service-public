package com.rarible.protocol.union.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.OrderEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class OutgoingOrderEventListener(
    private val eventsProducer: RaribleKafkaProducer<OrderEventDto>
) : OutgoingEventListener<OrderEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: OrderEventDto) {
        eventsProducer.send(KafkaEventFactory.orderEvent(event)).ensureSuccess()
        logger.info("Order Event sent: {}", event)
    }
}
