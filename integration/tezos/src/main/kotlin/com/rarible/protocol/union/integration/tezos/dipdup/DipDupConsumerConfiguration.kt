package com.rarible.protocol.union.integration.tezos.dipdup

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
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
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@DipDupConfiguration
@Import(DipDupApiConfiguration::class)
class DipDupConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: DipDupIntegrationProperties,
    private val consumerFactory: ConsumerFactory
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
        handler: DipDupOrderEventHandler
    ): RaribleKafkaConsumerWorker<DipDupOrder> {
        return createConsumer(
            topic = DipDupTopicProvider.getOrderTopic(env),
            handler = handler,
            valueClass = DipDupOrder::class.java,
            eventType = EventType.ORDER,
            deserializer = DipDupDeserializer.OrderJsonSerializer::class.java
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
        handler: DipDupActivityEventHandler
    ): RaribleKafkaConsumerWorker<DipDupActivity> {
        return createConsumer(
            topic = DipDupTopicProvider.getActivityTopic(env),
            handler = handler,
            valueClass = DipDupActivity::class.java,
            eventType = EventType.ACTIVITY,
            deserializer = DipDupDeserializer.ActivityJsonSerializer::class.java
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
        handler: DipDupCollectionEventHandler
    ): RaribleKafkaConsumerWorker<DipDupCollectionEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getCollectionTopic(env),
            handler = handler,
            valueClass = DipDupCollectionEvent::class.java,
            eventType = EventType.COLLECTION,
            deserializer = DipDupDeserializer.CollectionJsonSerializer::class.java
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
        handler: DipDupItemEventHandler
    ): RaribleKafkaConsumerWorker<DipDupItemEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getItemTopic(env),
            handler = handler,
            valueClass = DipDupItemEvent::class.java,
            eventType = EventType.ITEM,
            deserializer = DipDupDeserializer.ItemEventJsonSerializer::class.java
        )
    }

    @Bean
    fun dipDupItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): DipDupItemMetaEventHandler {
        return DipDupItemMetaEventHandler(handler)
    }

    @Bean
    fun dipDupItemMetaEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: BlockchainEventHandler<DipDupItemMetaEvent, UnionItemMetaEvent>
    ): RaribleKafkaConsumerWorker<DipDupItemMetaEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getItemMetaTopic(env),
            handler = handler,
            valueClass = DipDupItemMetaEvent::class.java,
            eventType = EventType.ITEM_META,
            deserializer = DipDupDeserializer.ItemMetaEventJsonSerializer::class.java
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
        handler: DipDupOwnershipEventHandler
    ): RaribleKafkaConsumerWorker<DipDupOwnershipEvent> {
        return createConsumer(
            topic = DipDupTopicProvider.getOwnershipTopic(env),
            handler = handler,
            valueClass = DipDupOwnershipEvent::class.java,
            eventType = EventType.OWNERSHIP,
            deserializer = DipDupDeserializer.OwnershipEventJsonSerializer::class.java
        )
    }

    private fun <B, U> createConsumer(
        topic: String,
        handler: BlockchainEventHandler<B, U>,
        valueClass: Class<B>,
        eventType: EventType,
        deserializer: Class<*>
    ): RaribleKafkaConsumerWorker<B> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = consumer.brokerReplicaSet!!,
            topic = topic,
            group = consumerFactory.consumerGroup(eventType),
            concurrency = workers.getOrDefault(eventType.value, 9),
            batchSize = batchSize,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = valueClass
        )
        val kafkaConsumerFactory = RaribleKafkaConsumerFactory(env, host, deserializer)
        return kafkaConsumerFactory.createWorker(settings, BlockchainEventHandlerWrapper(handler))
    }
}
