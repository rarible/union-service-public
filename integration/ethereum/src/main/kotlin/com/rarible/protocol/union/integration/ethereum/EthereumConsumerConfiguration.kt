package com.rarible.protocol.union.integration.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
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
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val daemon = properties.daemon

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    //-------------------- Handlers -------------------//

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

    //-------------------- Workers --------------------//

    @Bean
    @Qualifier("ethereum.nft.consumer.factory")
    fun ethereumNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    @Qualifier("ethereum.order.consumer.factory")
    fun ethereumOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    @Qualifier("ethereum.activity.consumer.factory")
    fun ethereumActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    fun ethereumItemWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup, Blockchain.ETHEREUM)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun ethereumOwnershipWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup, Blockchain.ETHEREUM)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun ethereumCollectionWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>
    ): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerFactory.collectionGroup, Blockchain.ETHEREUM)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        @Qualifier("ethereum.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): KafkaConsumerWorker<OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerFactory.orderGroup, Blockchain.ETHEREUM)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun ethereumAuctionWorker(
        @Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        @Qualifier("ethereum.auction.handler") handler: BlockchainEventHandler<AuctionEventDto, UnionAuctionEvent>
    ): KafkaConsumerWorker<AuctionEventDto> {
        val consumer = factory.createAuctionEventsConsumer(consumerFactory.auctionGroup, Blockchain.ETHEREUM)
        return consumerFactory.createAuctionConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        @Qualifier("ethereum.activity.handler") handler: BlockchainEventHandler<com.rarible.protocol.dto.ActivityDto, UnionActivity>
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup, Blockchain.ETHEREUM)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers, batchSize)
    }
}
