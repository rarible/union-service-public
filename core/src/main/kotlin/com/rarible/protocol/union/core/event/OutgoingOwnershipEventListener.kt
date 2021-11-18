package com.rarible.protocol.union.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "kafka")
class OutgoingOwnershipEventListener(
    private val eventsProducer: RaribleKafkaProducer<OwnershipEventDto>
) : OutgoingEventListener<OwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: OwnershipEventDto) {
        eventsProducer.send(KafkaEventFactory.ownershipEvent(event))
            .ensureSuccess()
        logger.info("Ownership Event sent: {}", event)
    }
}
