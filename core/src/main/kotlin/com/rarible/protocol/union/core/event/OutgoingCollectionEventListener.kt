package com.rarible.protocol.union.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.util.LogUtils.log
import com.rarible.protocol.union.dto.CollectionEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class OutgoingCollectionEventListener(
    private val eventsProducer: RaribleKafkaProducer<CollectionEventDto>
) : OutgoingEventListener<CollectionEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: CollectionEventDto) {
        eventsProducer.send(KafkaEventFactory.collectionEvent(event)).ensureSuccess()
        logger.info("Collection Event sent: {}", event.log())
    }
}
