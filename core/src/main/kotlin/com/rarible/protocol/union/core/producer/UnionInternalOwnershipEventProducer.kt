package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionOwnershipChangeEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.dto.OwnershipIdDto
import org.springframework.stereotype.Component

@Component
class UnionInternalOwnershipEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer
) : UnionInternalEventProducer<UnionOwnershipEvent>(eventProducer) {

    override fun getBlockchain(event: UnionOwnershipEvent) = event.ownershipId.blockchain
    override fun toMessage(event: UnionOwnershipEvent) = KafkaEventFactory.internalOwnershipEvent(event)

    suspend fun sendChangeEvents(ids: List<OwnershipIdDto>) = send(ids.map {
        UnionOwnershipChangeEvent(it, offchainEventMark("enrichment-in"))
    })

}
