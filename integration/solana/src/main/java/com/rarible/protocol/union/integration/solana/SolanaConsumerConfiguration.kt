package com.rarible.protocol.union.integration.solana

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.subscriber.SolanaEventsConsumerFactory
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.solana.converter.SolanaActivityConverter
import com.rarible.protocol.union.integration.solana.converter.SolanaOrderConverter
import com.rarible.protocol.union.integration.solana.event.SolanaActivityEventHandler
import com.rarible.protocol.union.integration.solana.event.SolanaCollectionEventHandler
import com.rarible.protocol.union.integration.solana.event.SolanaItemEventHandler
import com.rarible.protocol.union.integration.solana.event.SolanaItemMetaEventHandler
import com.rarible.protocol.union.integration.solana.event.SolanaOrderEventHandler
import com.rarible.protocol.union.integration.solana.event.SolanaOwnershipEventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SolanaConfiguration
@Import(SolanaApiConfiguration::class)
class SolanaConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: SolanaIntegrationProperties,
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
    fun solanaItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): SolanaItemEventHandler {
        return SolanaItemEventHandler(handler)
    }

    @Bean
    fun solanaTokenMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): SolanaItemMetaEventHandler {
        return SolanaItemMetaEventHandler(handler)
    }

    @Bean
    fun solanaOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): SolanaOwnershipEventHandler {
        return SolanaOwnershipEventHandler(handler)
    }

    @Bean
    fun solanaCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): SolanaCollectionEventHandler {
        return SolanaCollectionEventHandler(handler)
    }

    @Bean
    fun solanaOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: SolanaOrderConverter
    ): SolanaOrderEventHandler {
        return SolanaOrderEventHandler(handler, converter)
    }

    @Bean
    fun solanaActivityEventHandler(
        handler: IncomingEventHandler<ActivityDto>,
        converter: SolanaActivityConverter
    ): SolanaActivityEventHandler {
        return SolanaActivityEventHandler(handler, converter)
    }

    //-------------------- Workers --------------------//

    @Bean
    fun solanaNftIndexerConsumerFactory(): SolanaEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return SolanaEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    fun solanaItemWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<TokenEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<TokenEventDto> {
        val consumer = factory.createTokenEventConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun solanaItemMetaWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<TokenMetaEventDto, UnionItemMetaEvent>
    ): KafkaConsumerWorker<TokenMetaEventDto> {
        val consumer = factory.createTokenMetaEventConsumer(consumerFactory.itemMetaGroup)
        return consumerFactory.createItemMetaConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun solanaOwnershipWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<BalanceEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<BalanceEventDto> {
        val consumer = factory.createBalanceEventConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun solanaCollectionWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<com.rarible.protocol.solana.dto.CollectionEventDto, UnionCollectionEvent>
    ): KafkaConsumerWorker<com.rarible.protocol.solana.dto.CollectionEventDto> {
        val consumer = factory.createCollectionEventConsumer(consumerFactory.collectionGroup)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun solanaOrderWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<com.rarible.protocol.solana.dto.OrderEventDto, UnionOrderEvent>
    ): KafkaConsumerWorker<com.rarible.protocol.solana.dto.OrderEventDto> {
        val consumer = factory.createOrderEventConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun solanaActivityWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<com.rarible.protocol.solana.dto.ActivityDto, ActivityDto>
    ): KafkaConsumerWorker<com.rarible.protocol.solana.dto.ActivityDto> {
        val consumer = factory.createActivityEventConsumer(consumerFactory.activityGroup)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers, batchSize)
    }

}
