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
import com.rarible.protocol.union.integration.ethereum.event.PolygonActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@PolygonComponent
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [PolygonConsumerConfiguration::class])
@EnableConfigurationProperties(value = [PolygonIntegrationProperties::class])
@ConditionalOnProperty(name = ["integration.polygon.consumer.brokerReplicaSet"])
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

    // ------ POLYGON
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
        handler: PolygonItemEventHandler
    ): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup, Blockchain.POLYGON)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonCollectionWorker(
        @Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        handler: PolygonCollectionEventHandler
    ): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerFactory.collectionGroup, Blockchain.POLYGON)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonOwnershipWorker(
        @Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory,
        handler: PolygonOwnershipEventHandler
    ): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup, Blockchain.POLYGON)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonOrderWorker(
        @Qualifier("polygon.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        handler: PolygonOrderEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerFactory.orderGroup, Blockchain.POLYGON)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun polygonActivityWorker(
        @Qualifier("polygon.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        handler: PolygonActivityEventHandler
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup, Blockchain.POLYGON)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}