package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.BlockchainEventHandlerWrapper
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.stereotype.Component

@Component
class ConsumerFactory(
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory
) {

    fun <T> createBlockchainConsumerWorkerGroup(
        hosts: String,
        topic: String,
        handler: BlockchainEventHandler<T, *>,
        valueClass: Class<T>,
        workers: Map<String, Int>,
        eventType: EventType,
        batchSize: Int
    ): RaribleKafkaConsumerWorker<T> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = hosts,
            topic = topic,
            group = consumerGroup(eventType),
            concurrency = workers.getOrDefault(eventType.value, 1),
            batchSize = batchSize,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = valueClass
        )
        return kafkaConsumerFactory.createWorker(settings, BlockchainEventHandlerWrapper(handler))
    }

    fun consumerGroup(type: EventType): String {
        return "protocol.union.${type.value}"
    }
}
