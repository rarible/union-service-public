package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.converter.ItemEventToDtoConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderItemEventListener(
    private val eventsProducer: RaribleKafkaProducer<ItemEventDto>
) : ItemEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val eventsHeaders = mapOf(
        "protocol.union.item.event.version" to UnionEventTopicProvider.VERSION
    )

    override suspend fun onEvent(event: ItemEvent) {
        val dto = ItemEventToDtoConverter.convert(event)
        val message = KafkaMessage(
            id = dto.eventId,
            key = dto.itemId.fullId(),
            value = dto,
            headers = eventsHeaders
        )
        eventsProducer.send(message).ensureSuccess()
        logger.info("Item Event sent: {}", dto)
    }
}
