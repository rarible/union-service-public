package com.rarible.protocol.union.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.ActivityDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class OutgoingActivityEventListener(
    private val eventsProducer: RaribleKafkaProducer<ActivityDto>
) : OutgoingEventListener<ActivityDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: ActivityDto) {
        eventsProducer.send(KafkaEventFactory.activityEvent(event)).ensureSuccess()
        logger.debug("Activity Event sent: {}", event)
    }

    suspend fun onEvents(events: List<ActivityDto>) {
        val messages = events.map { KafkaEventFactory.activityEvent(it) }
        eventsProducer.send(messages)
    }
}
