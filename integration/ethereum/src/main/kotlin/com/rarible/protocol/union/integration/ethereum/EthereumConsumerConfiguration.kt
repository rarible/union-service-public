package com.rarible.protocol.union.integration.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.event.EthActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
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
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    //-------------------- Handlers -------------------//

    @Bean
    @Qualifier("ethereum.item.handler")
    fun ethereumItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthItemEventHandler(BlockchainDto.ETHEREUM, handler)
    }

    @Bean
    @Qualifier("ethereum.ownership.handler")
    fun ethereumOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return EthOwnershipEventHandler(BlockchainDto.ETHEREUM, handler)
    }

    @Bean
    @Qualifier("ethereum.collection.handler")
    fun ethereumCollectionEventHandler(handler: IncomingEventHandler<CollectionEventDto>): EthCollectionEventHandler {
        return EthCollectionEventHandler(BlockchainDto.ETHEREUM, handler)
    }

    @Bean
    @Qualifier("ethereum.order.handler")
    fun ethereumOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return EthOrderEventHandler(BlockchainDto.ETHEREUM, handler, converter)
    }

    @Bean
    @Qualifier("ethereum.activity.handler")
    fun ethereumActivityEventHandler(
        handler: IncomingEventHandler<ActivityDto>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return EthActivityEventHandler(BlockchainDto.ETHEREUM, handler, converter)
    }

    //-------------------- Workers --------------------//

    @Bean
    @Qualifier("ethereum.nft.consumer.factory")
    fun ethereumNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("ethereum.order.consumer.factory")
    fun ethereumOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("ethereum.activity.consumer.factory")
    fun ethereumActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun ethereumItemWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.item.handler") handler: EthItemEventHandler
    ): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup, Blockchain.POLYGON)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumOwnershipWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.ownership.handler") handler: EthOwnershipEventHandler
    ): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup, Blockchain.POLYGON)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }


    @Bean
    fun ethereumCollectionWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.collection.handler") handler: EthCollectionEventHandler
    ): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerFactory.collectionGroup, Blockchain.POLYGON)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        @Qualifier("ethereum.order.handler") handler: EthOrderEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerFactory.orderGroup, Blockchain.POLYGON)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        @Qualifier("ethereum.activity.handler") handler: EthActivityEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup, Blockchain.POLYGON)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}