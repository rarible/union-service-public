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

@PolygonConfiguration
@Import(PolygonApiConfiguration::class)
class PolygonConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: PolygonIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    //-------------------- Handlers -------------------//

    @Bean
    @Qualifier("polygon.item.handler")
    fun polygonItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthItemEventHandler(BlockchainDto.POLYGON, handler)
    }

    @Bean
    @Qualifier("polygon.ownership.handler")
    fun polygonOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return EthOwnershipEventHandler(BlockchainDto.POLYGON, handler)
    }

    @Bean
    @Qualifier("polygon.collection.handler")
    fun polygonCollectionEventHandler(handler: IncomingEventHandler<CollectionEventDto>): EthCollectionEventHandler {
        return EthCollectionEventHandler(BlockchainDto.POLYGON, handler)
    }

    @Bean
    @Qualifier("polygon.order.handler")
    fun polygonOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return EthOrderEventHandler(BlockchainDto.POLYGON, handler, converter)
    }

    @Bean
    @Qualifier("polygon.activity.handler")
    fun polygonActivityEventHandler(
        handler: IncomingEventHandler<ActivityDto>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return EthActivityEventHandler(BlockchainDto.POLYGON, handler, converter)
    }

    //-------------------- Workers --------------------//

    @Bean
    @Qualifier("polygon.nft.consumer.factory")
    fun polygonNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("polygon.order.consumer.factory")
    fun polygonOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("polygon.activity.consumer.factory")
    fun polygonActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun polygonItemWorker(
        @Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("polygon.item.handler") handler: EthItemEventHandler
    ): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup, Blockchain.POLYGON)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonOwnershipWorker(
        @Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("polygon.ownership.handler") handler: EthOwnershipEventHandler
    ): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup, Blockchain.POLYGON)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }


    @Bean
    fun polygonCollectionWorker(
        @Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("polygon.collection.handler") handler: EthCollectionEventHandler
    ): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerFactory.collectionGroup, Blockchain.POLYGON)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonOrderWorker(
        @Qualifier("polygon.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        @Qualifier("polygon.order.handler") handler: EthOrderEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerFactory.orderGroup, Blockchain.POLYGON)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonActivityWorker(
        @Qualifier("polygon.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        @Qualifier("polygon.activity.handler") handler: EthActivityEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup, Blockchain.POLYGON)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}