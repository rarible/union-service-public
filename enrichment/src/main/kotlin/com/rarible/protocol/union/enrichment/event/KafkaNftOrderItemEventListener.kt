package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.enrichment.converter.ItemEventToDtoConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderItemEventListener(
    private val eventsProducer: RaribleKafkaProducer<ItemEventDto>
) : ItemEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: ItemEvent) {
        val dto = ItemEventToDtoConverter.convert(event)
        eventsProducer.send(KafkaEventFactory.itemEvent(dto)).ensureSuccess()
        logger.info("Item Event sent: {}", dto)
    }
}
