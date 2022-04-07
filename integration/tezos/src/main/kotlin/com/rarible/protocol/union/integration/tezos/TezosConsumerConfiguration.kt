package com.rarible.protocol.union.integration.tezos

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.tezos.dto.TezosActivitySafeDto
import com.rarible.protocol.tezos.dto.TezosCollectionSafeEventDto
import com.rarible.protocol.tezos.dto.TezosItemSafeEventDto
import com.rarible.protocol.tezos.dto.TezosOrderSafeEventDto
import com.rarible.protocol.tezos.dto.TezosOwnershipSafeEventDto
import com.rarible.protocol.tezos.subscriber.TezosEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.integration.tezos.event.TezosActivityEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosCollectionEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosItemEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosOrderEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosOwnershipEventHandler
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@TezosConfiguration
@Import(TezosApiConfiguration::class)
class TezosConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: TezosIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    @Bean
    fun tezosConsumerFactory(): TezosEventsConsumerFactory {
        return TezosEventsConsumerFactory(
            consumer.brokerReplicaSet!!,
            host,
            env,
            StringUtils.trimToNull(consumer.username),
            StringUtils.trimToNull(consumer.password)
        )
    }

    //-------------------- Handlers -------------------//

    @Bean
    fun tezosItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): TezosItemEventHandler {
        return TezosItemEventHandler(handler)
    }

    @Bean
    fun tezosOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): TezosOwnershipEventHandler {
        return TezosOwnershipEventHandler(handler)
    }

    @Bean
    fun tezosCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): TezosCollectionEventHandler {
        return TezosCollectionEventHandler(handler)
    }

    @Bean
    fun tezosOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: TezosOrderConverter
    ): TezosOrderEventHandler {
        return TezosOrderEventHandler(handler, converter)
    }

    @Bean
    fun tezosActivityEventHandler(
        handler: IncomingEventHandler<com.rarible.protocol.union.dto.ActivityDto>,
        converter: TezosActivityConverter
    ): TezosActivityEventHandler {
        return TezosActivityEventHandler(handler, converter)
    }

    //-------------------- Workers --------------------//

    @Bean
    fun tezosItemWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosItemSafeEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<TezosItemSafeEventDto> {
        val consumer = factory.createItemConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosCollectionWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosCollectionSafeEventDto, UnionCollectionEvent>
    ): KafkaConsumerWorker<TezosCollectionSafeEventDto> {
        val consumer = factory.createCollectionConsumer(consumerFactory.itemGroup)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosOwnershipWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosOwnershipSafeEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<TezosOwnershipSafeEventDto> {
        val consumer = factory.createOwnershipConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosOrderWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosOrderSafeEventDto, UnionOrderEvent>
    ): KafkaConsumerWorker<TezosOrderSafeEventDto> {
        val consumer = factory.createOrderConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosActivityWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosActivitySafeDto, com.rarible.protocol.union.dto.ActivityDto>
    ): KafkaConsumerWorker<TezosActivitySafeDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}
