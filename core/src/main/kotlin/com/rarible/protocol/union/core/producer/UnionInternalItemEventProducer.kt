package com.rarible.protocol.union.core.producer

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.dto.ItemIdDto
import org.springframework.stereotype.Component

@Component
class UnionInternalItemEventProducer(
    eventProducer: UnionInternalBlockchainEventProducer,
    eventCountMetrics: EventCountMetrics
) : UnionInternalEventProducer<UnionItemEvent>(eventProducer, eventCountMetrics) {

    override fun getBlockchain(event: UnionItemEvent) = event.itemId.blockchain
    override fun getEventType(event: UnionItemEvent): EventType = EventType.ITEM
    override fun toMessage(event: UnionItemEvent) = KafkaEventFactory.internalItemEvent(event)

    suspend fun sendChangeEvent(id: ItemIdDto) = sendChangeEvents(listOf(id))
    suspend fun sendChangeEvents(ids: Collection<ItemIdDto>) = send(
        ids.map {
            UnionItemChangeEvent(it, offchainEventMark("enrichment-in"))
        }
    )
}
