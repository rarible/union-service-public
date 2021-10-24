package com.rarible.protocol.union.integration.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.integration.ethereum.event.EthereumActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EthereumComponent
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [EthereumConsumerConfiguration::class])
@EnableConfigurationProperties(value = [EthereumIntegrationProperties::class])
@ConditionalOnProperty(name = ["integration.ethereum.consumer.brokerReplicaSet"])
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

    // ------ ETHEREUM
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
        handler: EthereumItemEventHandler
    ): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup, Blockchain.ETHEREUM)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumCollectionWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        handler: EthereumCollectionEventHandler
    ): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerFactory.collectionGroup, Blockchain.ETHEREUM)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumOwnershipWorker(
        @Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        handler: EthereumOwnershipEventHandler
    ): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup, Blockchain.ETHEREUM)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        handler: EthereumOrderEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerFactory.orderGroup, Blockchain.ETHEREUM)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        handler: EthereumActivityEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup, Blockchain.ETHEREUM)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}