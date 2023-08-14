package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.dto.CollectionIdDto
import org.springframework.stereotype.Component

@Component
class UnionInternalCollectionEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer,
    eventCountMetrics: EventCountMetrics
) : UnionInternalEventProducer<UnionCollectionEvent>(eventProducer, eventCountMetrics) {

    override fun getBlockchain(event: UnionCollectionEvent) = event.collectionId.blockchain
    override fun getEventType(event: UnionCollectionEvent): EventType = EventType.COLLECTION

    override fun toMessage(event: UnionCollectionEvent) = KafkaEventFactory.internalCollectionEvent(event)

    suspend fun sendChangeEvent(id: CollectionIdDto) = sendChangeEvents(listOf(id))
    suspend fun sendChangeEvents(ids: Collection<CollectionIdDto>) = send(
        ids.map {
            UnionCollectionChangeEvent(it, offchainEventMark("enrichment-in"))
        }
    )
}
