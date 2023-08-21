package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.OrderEventDto
import org.slf4j.LoggerFactory

class OutgoingOrderEventListener(
    private val eventsProducer: RaribleKafkaProducer<OrderEventDto>
) : OutgoingEventListener<OrderEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: OrderEventDto) {
        eventsProducer.send(KafkaEventFactory.orderEvent(event)).ensureSuccess()
        logger.info("Order Event sent: {}", event)
    }
}
