package com.rarible.protocol.union.integration.flow

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.dto.FlowActivityEventDto
import com.rarible.protocol.dto.FlowActivityEventTopicProvider
import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.dto.FlowNftCollectionEventTopicProvider
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftOwnershipEventTopicProvider
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderEventTopicProvider
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.event.FlowActivityEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowCollectionEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowItemEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOrderEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOwnershipEventHandler
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@FlowConfiguration
@Import(FlowApiConfiguration::class)
class FlowConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: FlowIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    // -------------------- Handlers -------------------//

    @Bean
    fun flowItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): FlowItemEventHandler {
        return FlowItemEventHandler(handler)
    }

    @Bean
    fun flowOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): FlowOwnershipEventHandler {
        return FlowOwnershipEventHandler(handler)
    }

    @Bean
    fun flowCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): FlowCollectionEventHandler {
        return FlowCollectionEventHandler(handler)
    }

    @Bean
    fun flowOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: FlowOrderConverter
    ): FlowOrderEventHandler {
        return FlowOrderEventHandler(handler, converter)
    }

    @Bean
    fun flowActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: FlowActivityConverter
    ): FlowActivityEventHandler {
        return FlowActivityEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun flowItemWorker(
        handler: BlockchainEventHandler<FlowNftItemEventDto, UnionItemEvent>,
        itemContainerFactory: RaribleKafkaListenerContainerFactory<FlowNftItemEventDto>,
    ): ConcurrentMessageListenerContainer<String, FlowNftItemEventDto> {
        return createConsumer(
            topic = FlowNftItemEventTopicProvider.getTopic(env),
            handler = handler,
            eventType = EventType.ITEM,
            factory = itemContainerFactory,
        )
    }

    @Bean
    fun flowOwnershipWorker(
        handler: BlockchainEventHandler<FlowOwnershipEventDto, UnionOwnershipEvent>,
        ownershipContainerFactory: RaribleKafkaListenerContainerFactory<FlowOwnershipEventDto>,
    ): ConcurrentMessageListenerContainer<String, FlowOwnershipEventDto> {
        return createConsumer(
            topic = FlowNftOwnershipEventTopicProvider.getTopic(env),
            handler = handler,
            eventType = EventType.OWNERSHIP,
            factory = ownershipContainerFactory,
        )
    }

    @Bean
    fun flowCollectionWorker(
        handler: BlockchainEventHandler<FlowCollectionEventDto, UnionCollectionEvent>,
        collectionContainerFactory: RaribleKafkaListenerContainerFactory<FlowCollectionEventDto>,
    ): ConcurrentMessageListenerContainer<String, FlowCollectionEventDto> {
        return createConsumer(
            topic = FlowNftCollectionEventTopicProvider.getTopic(env),
            handler = handler,
            eventType = EventType.COLLECTION,
            factory = collectionContainerFactory,
        )
    }

    @Bean
    fun flowOrderWorker(
        handler: BlockchainEventHandler<FlowOrderEventDto, UnionOrderEvent>,
        orderContainerFactory: RaribleKafkaListenerContainerFactory<FlowOrderEventDto>,
    ): ConcurrentMessageListenerContainer<String, FlowOrderEventDto> {
        return createConsumer(
            topic = FlowOrderEventTopicProvider.getTopic(env),
            handler = handler,
            eventType = EventType.ORDER,
            factory = orderContainerFactory,
        )
    }

    @Bean
    fun flowActivityWorker(
        handler: BlockchainEventHandler<FlowActivityEventDto, UnionActivity>,
        activityContainerFactory: RaribleKafkaListenerContainerFactory<FlowActivityEventDto>,
    ): ConcurrentMessageListenerContainer<String, FlowActivityEventDto> {
        return createConsumer(
            topic = FlowActivityEventTopicProvider.getActivityTopic(env),
            handler = handler,
            eventType = EventType.ACTIVITY,
            factory = activityContainerFactory,
        )
    }

    @Bean
    fun flowItemContainerFactory(): RaribleKafkaListenerContainerFactory<FlowNftItemEventDto> =
        createContainerFactory(eventType = EventType.ITEM, valueClass = FlowNftItemEventDto::class.java)

    @Bean
    fun flowOwnershipContainerFactory(): RaribleKafkaListenerContainerFactory<FlowOwnershipEventDto> =
        createContainerFactory(eventType = EventType.OWNERSHIP, valueClass = FlowOwnershipEventDto::class.java)

    @Bean
    fun flowCollectionContainerFactory(): RaribleKafkaListenerContainerFactory<FlowCollectionEventDto> =
        createContainerFactory(eventType = EventType.COLLECTION, valueClass = FlowCollectionEventDto::class.java)

    @Bean
    fun flowOrderContainerFactory(): RaribleKafkaListenerContainerFactory<FlowOrderEventDto> =
        createContainerFactory(eventType = EventType.ORDER, valueClass = FlowOrderEventDto::class.java)

    @Bean
    fun flowActivityContainerFactory(): RaribleKafkaListenerContainerFactory<FlowActivityEventDto> =
        createContainerFactory(eventType = EventType.ORDER, valueClass = FlowActivityEventDto::class.java)

    fun <T> createContainerFactory(
        eventType: EventType,
        valueClass: Class<T>,
    ): RaribleKafkaListenerContainerFactory<T> = RaribleKafkaListenerContainerFactory(
        settings = RaribleKafkaContainerFactorySettings(
            hosts = consumer.brokerReplicaSet!!,
            valueClass = valueClass,
            concurrency = workers.getOrDefault(eventType.value, 9),
            batchSize = batchSize,
            deserializer = UnionKafkaJsonDeserializer::class.java,
        )
    )

    private fun <B, U> createConsumer(
        topic: String,
        handler: BlockchainEventHandler<B, U>,
        eventType: EventType,
        factory: RaribleKafkaListenerContainerFactory<B>,
    ): ConcurrentMessageListenerContainer<String, B> {
        return consumerFactory.createBlockchainConsumerWorkerGroup(
            topic = topic,
            handler = handler,
            eventType = eventType,
            factory = factory,
        )
    }
}
