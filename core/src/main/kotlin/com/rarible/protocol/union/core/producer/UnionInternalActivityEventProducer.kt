package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionActivity
import org.springframework.stereotype.Component

@Component
class UnionInternalActivityEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer
) : UnionInternalEventProducer<UnionActivity>(eventProducer) {

    override fun getBlockchain(event: UnionActivity) = event.id.blockchain
    override fun toMessage(event: UnionActivity) = KafkaEventFactory.internalActivityEvent(event)
}
