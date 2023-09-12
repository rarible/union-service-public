package com.rarible.protocol.union.integration.ethereum.blockchain.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.AuctionEventDto
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
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.event.EthActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthAuctionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumAuctionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumOwnershipEventHandler
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@EthereumConfiguration
@Import(EthereumApiConfiguration::class)
class EthereumConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: EthereumIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    private val blockchain = Blockchain.ETHEREUM

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("ethereum.item.handler")
    fun ethereumItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthereumItemEventHandler(handler)
    }

    @Bean
    @Qualifier("ethereum.ownership.handler")
    fun ethereumOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return EthereumOwnershipEventHandler(handler)
    }

    @Bean
    @Qualifier("ethereum.collection.handler")
    fun ethereumCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return EthereumCollectionEventHandler(handler)
    }

    @Bean
    @Qualifier("ethereum.order.handler")
    fun ethereumOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return EthereumOrderEventHandler(handler, converter)
    }

    @Bean
    @Qualifier("ethereum.auction.handler")
    fun ethereumAuctionEventHandler(
        handler: IncomingEventHandler<UnionAuctionEvent>,
        converter: EthAuctionConverter
    ): EthAuctionEventHandler {
        return EthereumAuctionEventHandler(handler, converter)
    }

    @Bean
    @Qualifier("ethereum.activity.handler")
    fun ethereumActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return EthereumActivityEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun ethereumItemWorker(
        @Qualifier("ethereum.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>,
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
    fun ethereumOwnershipWorker(
        @Qualifier("ethereum.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>,
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
    fun ethereumCollectionWorker(
        @Qualifier("ethereum.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>,
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
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>,
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
    fun ethereumAuctionWorker(
        @Qualifier("ethereum.auction.handler") handler: BlockchainEventHandler<AuctionEventDto, UnionAuctionEvent>,
        auctionContainerFactory: RaribleKafkaListenerContainerFactory<AuctionEventDto>,
    ): ConcurrentMessageListenerContainer<String, AuctionEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getAuctionUpdateTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.AUCTION,
            factory = auctionContainerFactory,
        )
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>,
        activityContainerFactory: RaribleKafkaListenerContainerFactory<EthActivityEventDto>,
    ): ConcurrentMessageListenerContainer<String, EthActivityEventDto> {
        return createConsumer(
            topic = ActivityTopicProvider.getActivityTopic(env, blockchain.value),
            handler = handler,
            eventType = EventType.ACTIVITY,
            factory = activityContainerFactory,
        )
    }

    @Bean
    fun ethereumOrderContainerFactory(): RaribleKafkaListenerContainerFactory<OrderEventDto> =
        createContainerFactory(
            eventType = EventType.ORDER,
            valueClass = OrderEventDto::class.java,
        )

    @Bean
    fun ethereumCollectionContainerFactory(): RaribleKafkaListenerContainerFactory<NftCollectionEventDto> =
        createContainerFactory(
            eventType = EventType.COLLECTION,
            valueClass = NftCollectionEventDto::class.java,
        )

    @Bean
    fun ethereumItemContainerFactory(): RaribleKafkaListenerContainerFactory<NftItemEventDto> =
        createContainerFactory(
            eventType = EventType.ITEM,
            valueClass = NftItemEventDto::class.java,
        )

    @Bean
    fun ethereumOwnershipContainerFactory(): RaribleKafkaListenerContainerFactory<NftOwnershipEventDto> =
        createContainerFactory(
            eventType = EventType.OWNERSHIP,
            valueClass = NftOwnershipEventDto::class.java,
        )

    @Bean
    fun ethereumAuctionContainerFactory(): RaribleKafkaListenerContainerFactory<AuctionEventDto> =
        createContainerFactory(
            eventType = EventType.AUCTION,
            valueClass = AuctionEventDto::class.java,
        )

    @Bean
    fun ethereumActivityContainerFactory(): RaribleKafkaListenerContainerFactory<EthActivityEventDto> =
        createContainerFactory(
            eventType = EventType.ACTIVITY,
            valueClass = EthActivityEventDto::class.java,
        )

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
