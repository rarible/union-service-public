package com.rarible.protocol.union.integration.tezos.dipdup

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.config.DipDupDeserializer
import com.rarible.dipdup.listener.config.DipDupEventsConsumerFactory
import com.rarible.dipdup.listener.config.DipDupTopicProvider
import com.rarible.dipdup.listener.model.DipDupCollectionEvent
import com.rarible.dipdup.listener.model.DipDupItemEvent
import com.rarible.dipdup.listener.model.DipDupItemMetaEvent
import com.rarible.dipdup.listener.model.DipDupOwnershipEvent
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.BlockchainEventHandlerWrapper
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupActivityEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupCollectionEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupItemEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupItemMetaEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOrderEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOwnershipEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupTransfersEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@DipDupConfiguration
@Import(DipDupApiConfiguration::class)
class DipDupConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: DipDupIntegrationProperties,
    private val consumerFactory: ConsumerFactory,
    private val eventCountMetrics: EventCountMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val daemon = properties.daemon

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    @Bean
    fun dipDupConsumerFactory(): DipDupEventsConsumerFactory {
        return DipDupEventsConsumerFactory(
            properties.consumer!!.brokerReplicaSet!!,
            host,
            env
        )
    }

    @Bean
    fun dipDupOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: DipDupOrderConverter,
        mapper: ObjectMapper
    ): DipDupOrderEventHandler {
        return DipDupOrderEventHandler(handler, converter, mapper, properties.marketplaces)
    }

    @Bean
    fun dipDupOrderEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupOrderEventHandler,
        orderContainerFactory: RaribleKafkaListenerContainerFactory<DipDupOrder>,
    ): ConcurrentMessageListenerContainer<String, DipDupOrder> {
        return createConsumer(
            topic = DipDupTopicProvider.getOrderTopic(env),
            handler = handler,
            eventType = EventType.ORDER,
            factory = orderContainerFactory,
        )
    }

    @Bean
    fun dipDupTransferEventHandler(
        ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
        ownershipService: TzktOwnershipService,
        itemHandler: IncomingEventHandler<UnionItemEvent>,
        itemService: TzktItemService
    ): DipDupTransfersEventHandler {
        return DipDupTransfersEventHandler(ownershipHandler, ownershipService, itemHandler, itemService)
    }

    @Bean
    fun dipDupActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: DipDupActivityConverter,
        transfersEventHandler: DipDupTransfersEventHandler,
        properties: DipDupIntegrationProperties,
        mapper: ObjectMapper
    ): DipDupActivityEventHandler {
        return DipDupActivityEventHandler(handler, converter, transfersEventHandler, properties, mapper)
    }

    @Bean
    fun dipDupActivityEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupActivityEventHandler,
        activityContainerFactory: RaribleKafkaListenerContainerFactory<DipDupActivity>,
    ): ConcurrentMessageListenerContainer<String, DipDupActivity> {
        return createConsumer(
            topic = DipDupTopicProvider.getActivityTopic(env),
            handler = handler,
            eventType = EventType.ACTIVITY,
            factory = activityContainerFactory,
        )
    }

    @Bean
    fun dipDupCollectionEventHandler(
        handler: IncomingEventHandler<UnionCollectionEvent>,
        mapper: ObjectMapper,
        tzktCollectionService: TzktCollectionService,
        properties: DipDupIntegrationProperties
    ): DipDupCollectionEventHandler {
        return DipDupCollectionEventHandler(handler, tzktCollectionService, mapper, properties)
    }

    @Bean
    fun dipDupCollectionEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupCollectionEventHandler,
        collectionContainerFactory: RaribleKafkaListenerContainerFactory<DipDupCollectionEvent>,
    ): ConcurrentMessageListenerContainer<String, DipDupCollectionEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getCollectionTopic(env),
            handler = handler,
            eventType = EventType.COLLECTION,
            factory = collectionContainerFactory,
        )
    }

    @Bean
    fun dipDupItemEventHandler(
        handler: IncomingEventHandler<UnionItemEvent>,
        mapper: ObjectMapper
    ): DipDupItemEventHandler {
        return DipDupItemEventHandler(handler, mapper)
    }

    @Bean
    fun dipDupItemEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupItemEventHandler,
        itemContainerFactory: RaribleKafkaListenerContainerFactory<DipDupItemEvent>,
    ): ConcurrentMessageListenerContainer<String, DipDupItemEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getItemTopic(env),
            handler = handler,
            eventType = EventType.ITEM,
            factory = itemContainerFactory,
        )
    }

    @Bean
    fun dipDupItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): DipDupItemMetaEventHandler {
        return DipDupItemMetaEventHandler(handler)
    }

    @Bean
    fun dipDupItemMetaEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: BlockchainEventHandler<DipDupItemMetaEvent, UnionItemMetaEvent>,
        itemMetaContainerFactory: RaribleKafkaListenerContainerFactory<DipDupItemMetaEvent>,
    ): ConcurrentMessageListenerContainer<String, DipDupItemMetaEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getItemMetaTopic(env),
            handler = handler,
            eventType = EventType.ITEM_META,
            factory = itemMetaContainerFactory,
        )
    }

    @Bean
    fun dipDupOwnershipEventHandler(
        handler: IncomingEventHandler<UnionOwnershipEvent>,
        mapper: ObjectMapper
    ): DipDupOwnershipEventHandler {
        return DipDupOwnershipEventHandler(handler, mapper)
    }

    @Bean
    fun dipDupOwnershipEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupOwnershipEventHandler,
        ownershipContainerFactory: RaribleKafkaListenerContainerFactory<DipDupOwnershipEvent>,
    ): ConcurrentMessageListenerContainer<String, DipDupOwnershipEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getOwnershipTopic(env),
            handler = handler,
            eventType = EventType.OWNERSHIP,
            factory = ownershipContainerFactory,
        )
    }

    @Bean
    fun didpdupOrderContainerFactory(): RaribleKafkaListenerContainerFactory<DipDupOrder> =
        createContainerFactory(
            eventType = EventType.ORDER,
            valueClass = DipDupOrder::class.java,
            deserializer = DipDupDeserializer.OrderJsonSerializer::class.java
        )

    @Bean
    fun didpdupActivityContainerFactory(): RaribleKafkaListenerContainerFactory<DipDupActivity> =
        createContainerFactory(
            eventType = EventType.ACTIVITY,
            valueClass = DipDupActivity::class.java,
            deserializer = DipDupDeserializer.ActivityJsonSerializer::class.java
        )

    @Bean
    fun didpdupItemContainerFactory(): RaribleKafkaListenerContainerFactory<DipDupItemEvent> =
        createContainerFactory(
            eventType = EventType.ITEM,
            valueClass = DipDupItemEvent::class.java,
            deserializer = DipDupDeserializer.ItemEventJsonSerializer::class.java
        )

    @Bean
    fun didpdupItemMetaContainerFactory(): RaribleKafkaListenerContainerFactory<DipDupItemMetaEvent> =
        createContainerFactory(
            eventType = EventType.ITEM_META,
            valueClass = DipDupItemMetaEvent::class.java,
            deserializer = DipDupDeserializer.ItemMetaEventJsonSerializer::class.java
        )

    @Bean
    fun didpdupOwnershipContainerFactory(): RaribleKafkaListenerContainerFactory<DipDupOwnershipEvent> =
        createContainerFactory(
            eventType = EventType.OWNERSHIP,
            valueClass = DipDupOwnershipEvent::class.java,
            deserializer = DipDupDeserializer.OwnershipEventJsonSerializer::class.java
        )

    @Bean
    fun didpdupCollectionContainerFactory(): RaribleKafkaListenerContainerFactory<DipDupCollectionEvent> =
        createContainerFactory(
            eventType = EventType.COLLECTION,
            valueClass = DipDupCollectionEvent::class.java,
            deserializer = DipDupDeserializer.CollectionJsonSerializer::class.java
        )

    fun <T> createContainerFactory(
        eventType: EventType,
        valueClass: Class<T>,
        deserializer: Class<*>,
    ): RaribleKafkaListenerContainerFactory<T> = RaribleKafkaListenerContainerFactory(
        settings = RaribleKafkaContainerFactorySettings(
            hosts = consumer.brokerReplicaSet!!,
            valueClass = valueClass,
            concurrency = workers.getOrDefault(eventType.value, 9),
            batchSize = batchSize,
            deserializer = deserializer,
        )
    )

    private fun <B, U> createConsumer(
        topic: String,
        handler: BlockchainEventHandler<B, U>,
        eventType: EventType,
        factory: RaribleKafkaListenerContainerFactory<B>,
    ): ConcurrentMessageListenerContainer<String, B> {
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = consumerFactory.consumerGroup(eventType),
            async = false,
        )
        val eventCounter =
            eventCountMetrics.eventReceivedGauge(EventCountMetrics.Stage.INDEXER, handler.blockchain, eventType)
        val kafkaConsumerFactory = RaribleKafkaConsumerFactory(env, host)
        return kafkaConsumerFactory.createWorker(
            settings,
            BlockchainEventHandlerWrapper(handler, eventCounter),
            factory
        )
    }
}
