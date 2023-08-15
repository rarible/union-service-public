package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionOrderEvent
import org.springframework.stereotype.Component

@Component
class UnionInternalOrderEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer,
    eventCountMetrics: EventCountMetrics
) : UnionInternalEventProducer<UnionOrderEvent>(eventProducer, eventCountMetrics) {

    override fun getBlockchain(event: UnionOrderEvent) = event.orderId.blockchain
    override fun toMessage(event: UnionOrderEvent) = KafkaEventFactory.internalOrderEvent(event)
    override fun getEventType(event: UnionOrderEvent): EventType = EventType.ORDER
}
