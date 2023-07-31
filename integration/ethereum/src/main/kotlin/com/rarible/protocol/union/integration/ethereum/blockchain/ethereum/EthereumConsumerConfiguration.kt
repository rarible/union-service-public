package com.rarible.protocol.union.integration.ethereum.blockchain.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityDto
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
import com.rarible.protocol.union.integration.ethereum.event.EthActivityLegacyEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthAuctionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumActivityLegacyEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumAuctionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

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

    @Bean
    @Deprecated("Remove later")
    @Qualifier("ethereum.activity.handler.legacy")
    fun ethereumLegacyActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityLegacyEventHandler {
        return EthereumActivityLegacyEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun ethereumItemWorker(
        @Qualifier("ethereum.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftItemEventDto::class.java,
            eventType = EventType.ITEM,
        )
    }

    @Bean
    fun ethereumOwnershipWorker(
        @Qualifier("ethereum.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        return createConsumer(
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftOwnershipEventDto::class.java,
            eventType = EventType.OWNERSHIP,
        )
    }

    @Bean
    fun ethereumCollectionWorker(
        @Qualifier("ethereum.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        return createConsumer(
            topic = NftCollectionEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftCollectionEventDto::class.java,
            eventType = EventType.COLLECTION,
        )
    }

    @Bean
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, blockchain.value),
            handler = handler,
            valueClass = OrderEventDto::class.java,
            eventType = EventType.ORDER,
        )
    }

    @Bean
    fun ethereumAuctionWorker(
        @Qualifier("ethereum.auction.handler") handler: BlockchainEventHandler<AuctionEventDto, UnionAuctionEvent>
    ): RaribleKafkaConsumerWorker<AuctionEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getAuctionUpdateTopic(env, blockchain.value),
            handler = handler,
            valueClass = AuctionEventDto::class.java,
            eventType = EventType.AUCTION,
        )
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>
    ): RaribleKafkaConsumerWorker<EthActivityEventDto> {
        return createConsumer(
            topic = ActivityTopicProvider.getActivityTopic(env, blockchain.value),
            handler = handler,
            valueClass = EthActivityEventDto::class.java,
            eventType = EventType.ACTIVITY,
        )
    }

    @Bean
    @Deprecated("Remove later")
    fun ethereumLegacyActivityWorker(
        @Qualifier("ethereum.activity.handler.legacy") handler: BlockchainEventHandler<ActivityDto, UnionActivity>
    ): RaribleKafkaConsumerWorker<ActivityDto> {
        return createConsumer(
            topic = ActivityTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = ActivityDto::class.java,
            eventType = EventType.ACTIVITY,
        )
    }

    private fun <B, U> createConsumer(
        topic: String,
        handler: BlockchainEventHandler<B, U>,
        valueClass: Class<B>,
        eventType: EventType
    ): RaribleKafkaConsumerWorker<B> {
        return consumerFactory.createBlockchainConsumerWorkerGroup(
            hosts = consumer.brokerReplicaSet!!,
            topic = topic,
            handler = handler,
            valueClass = valueClass,
            workers = workers,
            eventType = eventType,
            batchSize = batchSize
        )
    }
}
