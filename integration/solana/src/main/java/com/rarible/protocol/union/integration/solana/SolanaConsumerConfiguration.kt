package com.rarible.protocol.union.integration.solana

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.solana.event.SolanaItemEventHandler
import com.rarible.protocol.union.integration.solana.event.SolanaOwnershipEventHandler
import com.rarible.protocol.union.subscriber.SolanaEventsConsumerFactory
import com.rarible.solana.protocol.dto.BalanceEventDto
import com.rarible.solana.protocol.dto.TokenEventDto
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
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    //-------------------- Handlers -------------------//

    @Bean
    fun solanaItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): SolanaItemEventHandler {
        return SolanaItemEventHandler(handler)
    }

    @Bean
    fun solanaOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): SolanaOwnershipEventHandler {
        return SolanaOwnershipEventHandler(handler)
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
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun solanaOwnershipWorker(
        factory: SolanaEventsConsumerFactory,
        handler: BlockchainEventHandler<BalanceEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<BalanceEventDto> {
        val consumer = factory.createBalanceEventConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

}
