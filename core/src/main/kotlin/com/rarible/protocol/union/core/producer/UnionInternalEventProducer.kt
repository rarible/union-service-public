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
        try {
            eventCountMetrics.eventSent(EventCountMetrics.Stage.INTERNAL, getBlockchain(event), getEventType(event))
            eventProducer.getProducer(getBlockchain(event)).send(toMessage(event))
        } catch (e: Throwable) {
            eventCountMetrics.eventSent(EventCountMetrics.Stage.INTERNAL, getBlockchain(event), getEventType(event), -1)
            throw e
        }
    }

    suspend fun send(events: Collection<T>) {
        events.groupBy { getBlockchain(it) }.forEach { blockchainBatch ->
            val messages = blockchainBatch.value.map { toMessage(it) }
            try {
                eventCountMetrics.eventSent(
                    stage = EventCountMetrics.Stage.INTERNAL,
                    blockchain = blockchainBatch.key,
                    eventType = getEventType(blockchainBatch.value.first()),
                    count = messages.size
                )
                eventProducer.getProducer(blockchainBatch.key).send(messages).collect()
            } catch (e: Throwable) {
                eventCountMetrics.eventSent(
                    stage = EventCountMetrics.Stage.INTERNAL,
                    blockchain = blockchainBatch.key,
                    eventType = getEventType(blockchainBatch.value.first()),
                    count = -messages.size
                )
                throw e
            }
        }
    }

    abstract fun toMessage(event: T): KafkaMessage<UnionInternalBlockchainEvent>

    abstract fun getBlockchain(event: T): BlockchainDto

    abstract fun getEventType(event: T): EventType
}
