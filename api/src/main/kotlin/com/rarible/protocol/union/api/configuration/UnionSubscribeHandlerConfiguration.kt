package com.rarible.protocol.union.api.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeItemEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeOrderEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeOwnershipEventHandler
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.autoconfigure.UnionEventsSubscriberProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class UnionSubscribeHandlerConfiguration(
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory,
    private val properties: SubscribeProperties,
    private val unionSubscriberProperties: UnionEventsSubscriberProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name

    @Bean
    fun unionSubscribeItemWorker(
        handler: UnionSubscribeItemEventHandler
    ): RaribleKafkaConsumerWorker<ItemEventDto> {
        return createSubscribeWorker(
            type = EventType.ITEM,
            topic = UnionEventTopicProvider.getItemTopic(env),
            handler = handler,
            valueClass = ItemEventDto::class.java
        )
    }

    @Bean
    fun unionSubscribeOwnershipWorker(
        handler: UnionSubscribeOwnershipEventHandler
    ): RaribleKafkaConsumerWorker<OwnershipEventDto> {
        return createSubscribeWorker(
            type = EventType.OWNERSHIP,
            topic = UnionEventTopicProvider.getOwnershipTopic(env),
            handler = handler,
            valueClass = OwnershipEventDto::class.java
        )
    }

    @Bean
    fun unionSubscribeOrderWorker(
        handler: UnionSubscribeOrderEventHandler
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createSubscribeWorker(
            type = EventType.ORDER,
            topic = UnionEventTopicProvider.getOrderTopic(env),
            handler = handler,
            valueClass = OrderEventDto::class.java
        )
    }

    private fun <T> createSubscribeWorker(
        type: EventType,
        topic: String,
        handler: RaribleKafkaEventHandler<T>,
        valueClass: Class<T>
    ): RaribleKafkaConsumerWorker<T> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = unionSubscriberProperties.brokerReplicaSet,
            topic = topic,
            group = "${subscribeConsumerGroup(type)}.${UUID.randomUUID()}",
            concurrency = properties.workers.getOrDefault(type.value, 9),
            batchSize = 100,
            async = false,
            offsetResetStrategy = properties.offsetResetStrategy,
            valueClass = valueClass
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    private fun subscribeConsumerGroup(type: EventType): String {
        return "protocol.union.subscribe.${type.value}"
    }
}
