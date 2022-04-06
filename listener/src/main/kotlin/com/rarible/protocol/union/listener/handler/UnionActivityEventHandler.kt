package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionActivityEventHandler(
    private val eventsProducer: RaribleKafkaProducer<UnionWrappedEvent>
) : IncomingEventHandler<ActivityDto> {

    override suspend fun onEvent(event: ActivityDto) {
        eventsProducer.send(KafkaEventFactory.wrappedActivityEvent(event))
    }
}
