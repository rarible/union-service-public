package com.rarible.protocol.union.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.flow.collect

abstract class UnionInternalEventProducer<T>(
    private val eventProducer: UnionInternalBlockchainEventProducer
) {

    suspend fun send(event: T) {
        eventProducer.getProducer(getBlockchain(event))
            .send(toMessage(event))
    }

    suspend fun send(events: Collection<T>) {
        events.groupBy { getBlockchain(it) }.forEach { blockchainBatch ->
            val messages = blockchainBatch.value.map { toMessage(it) }
            eventProducer.getProducer(blockchainBatch.key).send(messages).collect()
        }
    }

    abstract fun toMessage(event: T): KafkaMessage<UnionInternalBlockchainEvent>

    abstract fun getBlockchain(event: T): BlockchainDto
}