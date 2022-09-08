package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.listener.handler.internal.IncomingBlockchainEventHandler
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionActivityEventHandler(
    eventProducer: UnionInternalBlockchainEventProducer
) : IncomingBlockchainEventHandler<ActivityDto>(eventProducer) {

    override fun toMessage(event: ActivityDto) = KafkaEventFactory.internalActivityEvent(event)
    override fun getBlockchain(event: ActivityDto) = event.id.blockchain

}
