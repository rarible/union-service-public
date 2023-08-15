package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionActivity
import org.springframework.stereotype.Component

@Component
class UnionInternalActivityEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer,
    eventCountMetrics: EventCountMetrics
) : UnionInternalEventProducer<UnionActivity>(eventProducer, eventCountMetrics) {

    override fun getBlockchain(event: UnionActivity) = event.id.blockchain
    override fun toMessage(event: UnionActivity) = KafkaEventFactory.internalActivityEvent(event)
    override fun getEventType(event: UnionActivity) = EventType.ACTIVITY
}
