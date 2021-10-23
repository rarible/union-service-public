package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.tezos.subscriber.TezosEventsConsumerFactory
import com.rarible.protocol.union.core.Entity
import com.rarible.protocol.union.core.IntegrationProperties
import com.rarible.protocol.union.core.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.core.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.core.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.core.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConfiguration
import com.rarible.protocol.union.listener.config.activity.EthActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.config.activity.FlowActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.handler.BatchedConsumerWorker
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import com.rarible.protocol.union.listener.handler.KafkaConsumerWorker
import com.rarible.protocol.union.listener.handler.ethereum.EthereumActivityEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCollectionEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumItemEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumOrderEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumOwnershipEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowActivityEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowItemEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowOrderEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowOwnershipEventHandler
import com.rarible.protocol.union.listener.handler.tezos.TezosActivityEventHandler
import com.rarible.protocol.union.listener.handler.tezos.TezosItemEventHandler
import com.rarible.protocol.union.listener.handler.tezos.TezosOrderEventHandler
import com.rarible.protocol.union.listener.handler.tezos.TezosOwnershipEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import io.micrometer.core.instrument.MeterRegistry
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(EnrichmentConfiguration::class)
@EnableRaribleTask
@EnableMongock
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val integrationProperties: IntegrationProperties,
    private val listenerProperties: UnionListenerProperties,
    private val meterRegistry: MeterRegistry,

    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val enrichmentOrderEventService: EnrichmentOrderEventService,

    private val itemProducer: RaribleKafkaProducer<ItemEventDto>,
    private val ownershipProducer: RaribleKafkaProducer<OwnershipEventDto>,
    private val orderProducer: RaribleKafkaProducer<OrderEventDto>,
    private val activityProducer: RaribleKafkaProducer<ActivityDto>,
    private val collectionProducer: RaribleKafkaProducer<CollectionEventDto>
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val ethereumProperties = integrationProperties.get(BlockchainDto.ETHEREUM).consumer!!
    private val polygonProperties = integrationProperties.get(BlockchainDto.POLYGON).consumer!!
    private val flowProperties = integrationProperties.get(BlockchainDto.FLOW).consumer!!
    private val tezosProperties = integrationProperties.get(BlockchainDto.TEZOS).consumer!!

    //------------------------ Eth Consumers ------------------------//

    // ------ ETHEREUM
    @Bean
    @Qualifier("ethereum.nft.consumer.factory")
    fun ethNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = ethereumProperties.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("ethereum.order.consumer.factory")
    fun ethOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = ethereumProperties.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("ethereum.activity.consumer.factory")
    fun ethActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = ethereumProperties.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun ethereumItemWorker(@Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.ETHEREUM)
        val handler = EthereumItemEventHandler(BlockchainDto.ETHEREUM, enrichmentItemEventService)
        return createBatchedConsumerWorker(consumer, handler, Entity.ITEM)
    }

    @Bean
    fun ethereumCollectionWorker(@Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerGroup(Entity.COLLECTION), Blockchain.ETHEREUM)
        val handler = EthereumCollectionEventHandler(BlockchainDto.ETHEREUM, collectionProducer)
        return createCollectionConsumer(consumer, handler)
    }

    @Bean
    fun ethereumOwnershipWorker(@Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.ETHEREUM)
        val handler = EthereumOwnershipEventHandler(BlockchainDto.ETHEREUM, enrichmentOwnershipEventService)
        return createOwnershipConsumer(consumer, handler)
    }

    @Bean
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        ethOrderConverter: EthOrderConverter
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.ETHEREUM)
        val handler = EthereumOrderEventHandler(
            BlockchainDto.ETHEREUM,
            orderProducer,
            enrichmentOrderEventService,
            ethOrderConverter
        )
        return createOrderConsumer(consumer, handler)
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        ethActivityConverter: EthActivityConverter
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.ETHEREUM)
        val handler = EthereumActivityEventHandler(BlockchainDto.ETHEREUM, activityProducer, ethActivityConverter)
        return createActivityConsumer(consumer, handler)
    }

    // ------ POLYGON
    @Bean
    @Qualifier("polygon.nft.consumer.factory")
    fun polygonNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = polygonProperties.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("polygon.order.consumer.factory")
    fun polygonOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = polygonProperties.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("polygon.activity.consumer.factory")
    fun polygonActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = polygonProperties.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun polygonItemWorker(@Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.POLYGON)
        val handler = EthereumItemEventHandler(BlockchainDto.POLYGON, enrichmentItemEventService)
        return createItemConsumer(consumer, handler)
    }

    @Bean
    fun polygonCollectionWorker(@Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerGroup(Entity.COLLECTION), Blockchain.POLYGON)
        val handler = EthereumCollectionEventHandler(BlockchainDto.POLYGON, collectionProducer)
        return createCollectionConsumer(consumer, handler)
    }

    @Bean
    fun polygonOwnershipWorker(@Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.POLYGON)
        val handler = EthereumOwnershipEventHandler(BlockchainDto.POLYGON, enrichmentOwnershipEventService)
        return createOwnershipConsumer(consumer, handler)
    }

    @Bean
    fun polygonOrderWorker(
        @Qualifier("polygon.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        ethOrderConverter: EthOrderConverter
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.POLYGON)
        val handler = EthereumOrderEventHandler(
            BlockchainDto.POLYGON,
            orderProducer,
            enrichmentOrderEventService,
            ethOrderConverter
        )
        return createOrderConsumer(consumer, handler)
    }

    @Bean
    fun polygonActivityWorker(
        @Qualifier("polygon.activity.consumer.factory") factory: EthActivityEventsConsumerFactory,
        ethActivityConverter: EthActivityConverter
    ): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.POLYGON)
        val handler = EthereumActivityEventHandler(BlockchainDto.POLYGON, activityProducer, ethActivityConverter)
        return createActivityConsumer(consumer, handler)
    }

    //------------------------ Flow Consumers ------------------------//

    @Bean
    fun flowActivityConsumerFactory(): FlowActivityEventsConsumerFactory {
        val replicaSet = flowProperties.brokerReplicaSet
        return FlowActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowNftIndexerConsumerFactory(): FlowNftIndexerEventsConsumerFactory {
        val replicaSet = flowProperties.brokerReplicaSet
        return FlowNftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowItemWorker(factory: FlowNftIndexerEventsConsumerFactory): KafkaConsumerWorker<FlowNftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM))
        val handler = FlowItemEventHandler(BlockchainDto.FLOW, enrichmentItemEventService)
        return createItemConsumer(consumer, handler)
    }

    // TODO: Flow will support events on collections => create a worker here.

    @Bean
    fun flowOwnershipWorker(factory: FlowNftIndexerEventsConsumerFactory): KafkaConsumerWorker<FlowOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP))
        val handler = FlowOwnershipEventHandler(BlockchainDto.FLOW, enrichmentOwnershipEventService)
        return createOwnershipConsumer(consumer, handler)
    }

    @Bean
    fun flowOrderWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        flowOrderConverter: FlowOrderConverter
    ): KafkaConsumerWorker<FlowOrderEventDto> {
        val consumer = factory.createORderEventsConsumer(consumerGroup(Entity.ORDER))
        val handler = FlowOrderEventHandler(
            BlockchainDto.FLOW,
            orderProducer,
            enrichmentOrderEventService,
            flowOrderConverter
        )
        return createOrderConsumer(consumer, handler)
    }

    @Bean
    fun flowActivityWorker(
        factory: FlowActivityEventsConsumerFactory,
        flowActivityConverter: FlowActivityConverter
    ): KafkaConsumerWorker<FlowActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        val handler = FlowActivityEventHandler(BlockchainDto.FLOW, activityProducer, flowActivityConverter)
        return createActivityConsumer(consumer, handler)
    }

    //------------------------ Tezos Consumers ------------------------//
    @Bean
    fun tezosActivityConsumerFactory(): TezosEventsConsumerFactory {
        val replicaSet = tezosProperties.brokerReplicaSet
        return TezosEventsConsumerFactory(
            replicaSet,
            host,
            env,
            StringUtils.trimToNull(tezosProperties.username),
            StringUtils.trimToNull(tezosProperties.password)
        )
    }

    // TODO: Tezos will support events on collections => create a worker here.

    @Bean
    fun tezosItemWorker(factory: TezosEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.ItemEventDto> {
        val consumer = factory.createItemConsumer(consumerGroup(Entity.ITEM))
        val handler = TezosItemEventHandler(BlockchainDto.TEZOS, enrichmentItemEventService)
        return createItemConsumer(consumer, handler)
    }

    @Bean
    fun tezosOwnershipWorker(factory: TezosEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.OwnershipEventDto> {
        val consumer = factory.createOwnershipConsumer(consumerGroup(Entity.OWNERSHIP))
        val handler = TezosOwnershipEventHandler(BlockchainDto.TEZOS, enrichmentOwnershipEventService)
        return createOwnershipConsumer(consumer, handler)
    }

    @Bean
    fun tezosOrderWorker(
        factory: TezosEventsConsumerFactory,
        tezosOrderConverter: TezosOrderConverter
    ): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.OrderEventDto> {
        val consumer = factory.createOrderConsumer(consumerGroup(Entity.ORDER))
        val handler = TezosOrderEventHandler(
            BlockchainDto.TEZOS,
            orderProducer,
            enrichmentOrderEventService,
            tezosOrderConverter
        )
        return createOrderConsumer(consumer, handler)
    }

    @Bean
    fun tezosActivityWorker(
        factory: TezosEventsConsumerFactory,
        tezosActivityConverter: TezosActivityConverter
    ): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        val handler = TezosActivityEventHandler(BlockchainDto.TEZOS, activityProducer, tezosActivityConverter)
        return createActivityConsumer(consumer, handler)
    }

    private fun <T> createItemConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T>
    ): BatchedConsumerWorker<T> {
        return createBatchedConsumerWorker(consumer, handler, Entity.ITEM)
    }

    private fun <T> createOwnershipConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T>
    ): BatchedConsumerWorker<T> {
        return createBatchedConsumerWorker(consumer, handler, Entity.OWNERSHIP)
    }

    private fun <T> createOrderConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T>
    ): BatchedConsumerWorker<T> {
        return createBatchedConsumerWorker(consumer, handler, Entity.ORDER)
    }

    private fun <T> createActivityConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T>
    ): BatchedConsumerWorker<T> {
        return createBatchedConsumerWorker(consumer, handler, Entity.ACTIVITY)
    }

    private fun <T> createCollectionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T>
    ): BatchedConsumerWorker<T> {
        return createBatchedConsumerWorker(consumer, handler, Entity.COLLECTION)
    }

    private fun <T> createBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T>,
        entityType: String
    ): BatchedConsumerWorker<T> {
        val blockchain = handler.blockchain
        val properties = integrationProperties.get(blockchain)
        val workerCount = properties.consumer!!.workers.getOrDefault(entityType, 1)
        val workers = (1..workerCount).map {
            ConsumerWorker(
                consumer = consumer,
                properties = listenerProperties.monitoringWorker,
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "${blockchain.name.toLowerCase()}-${entityType}-$it"
            )
        }
        return BatchedConsumerWorker(properties.enabled, workers)
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }
}
