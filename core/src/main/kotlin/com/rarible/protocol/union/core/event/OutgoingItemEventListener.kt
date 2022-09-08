package com.rarible.protocol.union.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.util.LogUtils.log
import com.rarible.protocol.union.dto.ItemEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class OutgoingItemEventListener(
    private val eventsProducer: RaribleKafkaProducer<ItemEventDto>
) : OutgoingEventListener<ItemEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: ItemEventDto) {
        eventsProducer.send(KafkaEventFactory.itemEvent(event)).ensureSuccess()
        logger.info("Item Event sent: {}", event.log())
    }
}
