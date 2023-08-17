package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.slf4j.LoggerFactory

class OutgoingOwnershipEventListener(
    private val eventsProducer: RaribleKafkaProducer<OwnershipEventDto>
) : OutgoingEventListener<OwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: OwnershipEventDto) {
        eventsProducer.send(KafkaEventFactory.ownershipEvent(event))
            .ensureSuccess()
        logger.info("Ownership Event sent for item {}: {}", event.ownershipId.getItemId().fullId(), event)
    }
}
