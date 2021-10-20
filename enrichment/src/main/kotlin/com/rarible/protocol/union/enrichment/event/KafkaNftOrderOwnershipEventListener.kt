package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.converter.OwnershipEventToDtoConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderOwnershipEventListener(
    private val eventsProducer: RaribleKafkaProducer<OwnershipEventDto>
) : OwnershipEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: OwnershipEvent) {
        val dto = OwnershipEventToDtoConverter.convert(event)
        eventsProducer.send(KafkaEventFactory.ownershipEvent(dto)).ensureSuccess()
        logger.info("Ownership Event sent: {}", dto)
    }
}
