package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.BlockchainDto

abstract class IncomingBlockchainEventHandler<T>(
    private val eventProducer: UnionInternalBlockchainEventProducer
) : IncomingEventHandler<T> {

    override suspend fun onEvent(event: T) {
        eventProducer.getProducer(getBlockchain(event))
            .send(toMessage(event))
    }

    override suspend fun onEvents(events: Collection<T>) {
        events.groupBy { getBlockchain(it) }.forEach { blockchainBatch ->
            val messages = blockchainBatch.value.map { toMessage(it) }
            eventProducer.getProducer(blockchainBatch.key).send(messages)
        }
    }

    abstract fun toMessage(event: T): KafkaMessage<UnionInternalBlockchainEvent>

    abstract fun getBlockchain(event: T): BlockchainDto
}