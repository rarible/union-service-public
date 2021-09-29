package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.converter.OwnershipEventToDtoConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderOwnershipEventListener(
    private val eventsProducer: RaribleKafkaProducer<OwnershipEventDto>
) : OwnershipEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val eventsHeaders = mapOf(
        "protocol.union.ownership.event.version" to UnionEventTopicProvider.VERSION
    )

    override suspend fun onEvent(event: OwnershipEvent) {

        val dto = OwnershipEventToDtoConverter.convert(event)
        val itemId = ItemIdDto(dto.ownershipId.blockchain, dto.ownershipId.token, dto.ownershipId.tokenId)

        val message = KafkaMessage(
            id = dto.eventId,
            key = itemId.fullId(),
            value = dto,
            headers = eventsHeaders
        )
        eventsProducer.send(message).ensureSuccess()
        logger.info("Ownership Event sent: {}", dto)
    }
}
