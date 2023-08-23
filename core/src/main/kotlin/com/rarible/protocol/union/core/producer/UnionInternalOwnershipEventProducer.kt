package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionOwnershipChangeEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.dto.OwnershipIdDto
import org.springframework.stereotype.Component

@Component
class UnionInternalOwnershipEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer,
    eventCountMetrics: EventCountMetrics
) : UnionInternalEventProducer<UnionOwnershipEvent>(eventProducer, eventCountMetrics) {

    override fun getBlockchain(event: UnionOwnershipEvent) = event.ownershipId.blockchain
    override fun getEventType(event: UnionOwnershipEvent): EventType = EventType.OWNERSHIP
    override fun toMessage(event: UnionOwnershipEvent) = KafkaEventFactory.internalOwnershipEvent(event)

    suspend fun sendChangeEvent(id: OwnershipIdDto) = sendChangeEvents(listOf(id))
    suspend fun sendChangeEvents(ids: List<OwnershipIdDto>) = send(
        ids.map {
            UnionOwnershipChangeEvent(it, offchainEventMark("enrichment-in"))
        }
    )
}
