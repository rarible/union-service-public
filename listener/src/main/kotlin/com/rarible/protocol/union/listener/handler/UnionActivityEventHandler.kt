package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionActivityEventHandler(
    private val eventProducer: UnionInternalBlockchainEventProducer
) : IncomingEventHandler<ActivityDto> {

    override suspend fun onEvent(event: ActivityDto) {
        eventProducer.getProducer(event.id.blockchain)
            .send(KafkaEventFactory.internalActivityEvent(event))
    }
}
