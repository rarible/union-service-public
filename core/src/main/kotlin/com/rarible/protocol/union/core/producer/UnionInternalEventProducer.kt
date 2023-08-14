package com.rarible.protocol.union.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.flow.collect

abstract class UnionInternalEventProducer<T>(
    private val eventProducer: UnionInternalBlockchainEventProducer,
    private val eventCountMetrics: EventCountMetrics
) {

    suspend fun send(event: T) {
        eventProducer.getProducer(getBlockchain(event))
            .send(toMessage(event))
        eventCountMetrics.eventSent(EventCountMetrics.Stage.INTERNAL, getBlockchain(event), getEventType(event))
    }

    suspend fun send(events: Collection<T>) {
        events.groupBy { getBlockchain(it) }.forEach { blockchainBatch ->
            val messages = blockchainBatch.value.map { toMessage(it) }
            eventProducer.getProducer(blockchainBatch.key).send(messages).collect()
            eventCountMetrics.eventSent(
                stage = EventCountMetrics.Stage.INTERNAL,
                blockchain = blockchainBatch.key,
                eventType = getEventType(blockchainBatch.value.first()),
                count = messages.size
            )
        }
    }

    abstract fun toMessage(event: T): KafkaMessage<UnionInternalBlockchainEvent>

    abstract fun getBlockchain(event: T): BlockchainDto

    abstract fun getEventType(event: T): EventType
}
