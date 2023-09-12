package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.BlockchainEventHandlerWrapper
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class ConsumerFactory(
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory,
    private val eventCountMetrics: EventCountMetrics
) {

    fun <T> createBlockchainConsumerWorkerGroup(
        topic: String,
        handler: BlockchainEventHandler<T, *>,
        eventType: EventType,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = consumerGroup(eventType),
            async = false,
        )
        val eventCounter =
            eventCountMetrics.eventReceivedGauge(EventCountMetrics.Stage.INDEXER, handler.blockchain, eventType)
        return kafkaConsumerFactory.createWorker(
            settings,
            BlockchainEventHandlerWrapper(handler, eventCounter),
            factory
        )
    }

    fun consumerGroup(type: EventType): String {
        return "protocol.union.${type.value}"
    }
}
