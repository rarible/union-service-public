package com.rarible.protocol.union.api.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.union.api.handler.UnionSubscribeItemEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeOrderEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeOwnershipEventHandler
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.autoconfigure.UnionEventsSubscriberProperties
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.util.UUID

@Configuration
@EnableConfigurationProperties(value = [SubscribeProperties::class])
class UnionSubscribeHandlerConfiguration(
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory,
    private val properties: SubscribeProperties,
    private val unionSubscriberProperties: UnionEventsSubscriberProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name

    @Bean
    fun unionSubscribeItemWorker(
        handler: UnionSubscribeItemEventHandler,
        itemContainerFactory: RaribleKafkaListenerContainerFactory<ItemEventDto>,
    ): ConcurrentMessageListenerContainer<String, ItemEventDto> {
        return createSubscribeWorker(
            type = EventType.ITEM,
            topic = UnionEventTopicProvider.getItemTopic(env),
            handler = handler,
            factory = itemContainerFactory,
        )
    }

    @Bean
    fun unionSubscribeOwnershipWorker(
        handler: UnionSubscribeOwnershipEventHandler,
        ownershipContainerFactory: RaribleKafkaListenerContainerFactory<OwnershipEventDto>
    ): ConcurrentMessageListenerContainer<String, OwnershipEventDto> {
        return createSubscribeWorker(
            type = EventType.OWNERSHIP,
            topic = UnionEventTopicProvider.getOwnershipTopic(env),
            handler = handler,
            factory = ownershipContainerFactory,
        )
    }

    @Bean
    fun unionSubscribeOrderWorker(
        handler: UnionSubscribeOrderEventHandler,
        orderContainerFactory: RaribleKafkaListenerContainerFactory<OrderEventDto>,
    ): ConcurrentMessageListenerContainer<String, OrderEventDto> {
        return createSubscribeWorker(
            type = EventType.ORDER,
            topic = UnionEventTopicProvider.getOrderTopic(env),
            handler = handler,
            factory = orderContainerFactory,
        )
    }

    @Bean
    fun orderContainerFactory(): RaribleKafkaListenerContainerFactory<OrderEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = unionSubscriberProperties.brokerReplicaSet,
                concurrency = properties.workers.getOrDefault(EventType.ORDER.value, 9),
                batchSize = 100,
                offsetResetStrategy = OffsetResetStrategy.LATEST,
                valueClass = OrderEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun itemContainerFactory(): RaribleKafkaListenerContainerFactory<ItemEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = unionSubscriberProperties.brokerReplicaSet,
                concurrency = properties.workers.getOrDefault(EventType.ITEM.value, 9),
                batchSize = 100,
                offsetResetStrategy = OffsetResetStrategy.LATEST,
                valueClass = ItemEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun ownershipContainerFactory(): RaribleKafkaListenerContainerFactory<OwnershipEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = unionSubscriberProperties.brokerReplicaSet,
                concurrency = properties.workers.getOrDefault(EventType.OWNERSHIP.value, 9),
                batchSize = 100,
                offsetResetStrategy = OffsetResetStrategy.LATEST,
                valueClass = OwnershipEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    private fun <T> createSubscribeWorker(
        type: EventType,
        topic: String,
        handler: RaribleKafkaEventHandler<T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = "${subscribeConsumerGroup(type)}.${UUID.randomUUID()}",
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, factory)
    }

    private fun subscribeConsumerGroup(type: EventType): String {
        return "protocol.union.subscribe.${type.value}"
    }
}
