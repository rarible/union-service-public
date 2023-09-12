package com.rarible.protocol.union.integration.ethereum.blockchain.mantle

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
import com.rarible.protocol.union.integration.ethereum.event.MantleActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@MantleConfiguration
@Import(MantleApiConfiguration::class)
class MantleConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: MantleIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    private val blockchain = Blockchain.MANTLE

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("mantle.item.handler")
    fun mantleItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return MantleItemEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.ownership.handler")
    fun mantleOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return MantleOwnershipEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.collection.handler")
    fun mantleCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return MantleCollectionEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.order.handler")
    fun mantleOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return MantleOrderEventHandler(handler, converter)
    }

    @Bean
    @Qualifier("mantle.activity.handler")
    fun mantleActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return MantleActivityEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun mantleItemWorker(
        @Qualifier("mantle.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>,
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
    fun mantleOwnershipWorker(
        @Qualifier("mantle.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>,
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
    fun mantleCollectionWorker(
        @Qualifier("mantle.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>,
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
    fun mantleOrderWorker(
        @Qualifier("mantle.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>,
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
    fun mantleActivityWorker(
        @Qualifier("mantle.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>,
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
