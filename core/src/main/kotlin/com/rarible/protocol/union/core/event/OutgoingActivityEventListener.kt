package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.ActivityEvent
import com.rarible.protocol.union.dto.ActivityDto
import org.slf4j.LoggerFactory

class OutgoingActivityEventListener(
    private val eventsProducer: RaribleKafkaProducer<ActivityDto>
) : OutgoingEventListener<ActivityEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: ActivityEvent) {
        eventsProducer.send(KafkaEventFactory.activityEvent(event.activity)).ensureSuccess()
        logger.info("Activity Event sent: {}", event.activity)
    }
}
