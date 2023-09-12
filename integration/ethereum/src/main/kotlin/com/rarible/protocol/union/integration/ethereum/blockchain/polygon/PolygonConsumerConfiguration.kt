package com.rarible.protocol.union.integration.ethereum.blockchain.polygon

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.event.EthActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@PolygonConfiguration
@Import(PolygonApiConfiguration::class)
class PolygonConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: PolygonIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    private val blockchain = Blockchain.POLYGON

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("polygon.item.handler")
    fun polygonItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return PolygonItemEventHandler(handler)
    }

    @Bean
    @Qualifier("polygon.ownership.handler")
    fun polygonOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return PolygonOwnershipEventHandler(handler)
    }

    @Bean
    @Qualifier("polygon.collection.handler")
    fun polygonCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return PolygonCollectionEventHandler(handler)
    }

    @Bean
    @Qualifier("polygon.order.handler")
    fun polygonOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return PolygonOrderEventHandler(handler, converter)
    }

    @Bean
    @Qualifier("polygon.activity.handler")
    fun polygonActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return PolygonActivityEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun polygonItemWorker(
        @Qualifier("polygon.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>,
        itemContainerFactory: RaribleKafkaListenerContainerFactory<NftItemEventDto>,
    ): ConcurrentMessageListenerContainer<String, NftItemEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.ITEM,
            factory = itemContainerFactory,
        )
    }

    @Bean
    fun polygonOwnershipWorker(
        @Qualifier("polygon.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>,
        ownershipContainerFactory: RaribleKafkaListenerContainerFactory<NftOwnershipEventDto>,
    ): ConcurrentMessageListenerContainer<String, NftOwnershipEventDto> {
        return createConsumer(
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.OWNERSHIP,
            factory = ownershipContainerFactory,
        )
    }

    @Bean
    fun polygonCollectionWorker(
        @Qualifier("polygon.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>,
        collectionContainerFactory: RaribleKafkaListenerContainerFactory<NftCollectionEventDto>,
    ): ConcurrentMessageListenerContainer<String, NftCollectionEventDto> {
        return createConsumer(
            topic = NftCollectionEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.COLLECTION,
            factory = collectionContainerFactory,
        )
    }

    @Bean
    fun polygonOrderWorker(
        @Qualifier("polygon.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>,
        orderContainerFactory: RaribleKafkaListenerContainerFactory<OrderEventDto>,
    ): ConcurrentMessageListenerContainer<String, OrderEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.ORDER,
            factory = orderContainerFactory,
        )
    }

    @Bean
    fun polygonActivityWorker(
        @Qualifier("polygon.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>,
        activityContainerFactory: RaribleKafkaListenerContainerFactory<EthActivityEventDto>,
    ): ConcurrentMessageListenerContainer<String, EthActivityEventDto> {
        return createConsumer(
            topic = ActivityTopicProvider.getActivityTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.ACTIVITY,
            factory = activityContainerFactory,
        )
    }

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
