package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.util.LogUtils.log
import com.rarible.protocol.union.dto.ItemEventDto
import org.slf4j.LoggerFactory

class OutgoingItemEventListener(
    private val eventsProducer: RaribleKafkaProducer<ItemEventDto>
) : OutgoingEventListener<ItemEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: ItemEventDto) {
        eventsProducer.send(KafkaEventFactory.itemEvent(event)).ensureSuccess()
        logger.info("Item Event sent: {}", event.log())
    }
}
