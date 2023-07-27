package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.dto.CollectionIdDto
import org.springframework.stereotype.Component

@Component
class UnionInternalCollectionEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer
) : UnionInternalEventProducer<UnionCollectionEvent>(eventProducer) {

    override fun getBlockchain(event: UnionCollectionEvent) = event.collectionId.blockchain
    override fun toMessage(event: UnionCollectionEvent) = KafkaEventFactory.internalCollectionEvent(event)

    suspend fun sendChangeEvent(id: CollectionIdDto) = sendChangeEvents(listOf(id))
    suspend fun sendChangeEvents(ids: Collection<CollectionIdDto>) = send(
        ids.map {
            UnionCollectionChangeEvent(it, offchainEventMark("enrichment-in"))
        }
    )
}
