package com.rarible.protocol.union.integration.solana

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.SolanaEventTopicProvider
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.subscriber.SolanaEventsConsumerFactory
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
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
        handler: IncomingEventHandler<UnionActivity>,
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
    ): RaribleKafkaConsumerWorker<TokenEventDto> {
        return createConsumer(
            topic = SolanaEventTopicProvider.getTokenTopic(env),
            handler = handler,
            valueClass = TokenEventDto::class.java,
            eventType = EventType.ITEM_META,
        )
    }

    @Bean
    fun solanaItemMetaWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<TokenMetaEventDto, UnionItemMetaEvent>
    ): RaribleKafkaConsumerWorker<TokenMetaEventDto> {
        return createConsumer(
            topic = SolanaEventTopicProvider.getTokenMetaTopic(env),
            handler = handler,
            valueClass = TokenMetaEventDto::class.java,
            eventType = EventType.ITEM_META,
        )
    }

    @Bean
    fun solanaOwnershipWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<BalanceEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<BalanceEventDto> {
        return createConsumer(
            topic = SolanaEventTopicProvider.getBalanceTopic(env),
            handler = handler,
            valueClass = BalanceEventDto::class.java,
            eventType = EventType.OWNERSHIP,
        )
    }

    @Bean
    fun solanaCollectionWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<com.rarible.protocol.solana.dto.CollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<com.rarible.protocol.solana.dto.CollectionEventDto> {
        return createConsumer(
            topic = SolanaEventTopicProvider.getCollectionTopic(env),
            handler = handler,
            valueClass = com.rarible.protocol.solana.dto.CollectionEventDto::class.java,
            eventType = EventType.COLLECTION,
        )
    }

    @Bean
    fun solanaOrderWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<com.rarible.protocol.solana.dto.OrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<com.rarible.protocol.solana.dto.OrderEventDto> {
        return createConsumer(
            topic = SolanaEventTopicProvider.getOrderTopic(env),
            handler = handler,
            valueClass = com.rarible.protocol.solana.dto.OrderEventDto::class.java,
            eventType = EventType.ORDER,
        )
    }

    @Bean
    fun solanaActivityWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<com.rarible.protocol.solana.dto.ActivityDto, UnionActivity>
    ): RaribleKafkaConsumerWorker<com.rarible.protocol.solana.dto.ActivityDto> {
        return createConsumer(
            topic = SolanaEventTopicProvider.getActivityTopic(env),
            handler = handler,
            valueClass = com.rarible.protocol.solana.dto.ActivityDto::class.java,
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
