package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.converter.CollectionEventToDtoConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderCollectionEventListener(
    private val eventsProducer: RaribleKafkaProducer<CollectionEventDto>
) : CollectionEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val eventsHeaders = mapOf(
        "protocol.union.collection.event.version" to UnionEventTopicProvider.VERSION
    )

    override suspend fun onEvent(event: CollectionEvent) {
        val dto = CollectionEventToDtoConverter.convert(event)
        val message = KafkaMessage(
            id = dto.eventId,
            key = dto.collectionId.fullId(),
            value = dto,
            headers = eventsHeaders
        )
        eventsProducer.send(message).ensureSuccess()
        logger.info("Collection Event sent: {}", dto)
    }
}
