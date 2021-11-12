package com.rarible.protocol.union.integration.tezos

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.tezos.dto.ActivityDto
import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.tezos.dto.OrderEventDto
import com.rarible.protocol.tezos.dto.OwnershipEventDto
import com.rarible.protocol.tezos.subscriber.TezosEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.integration.tezos.event.TezosActivityEventHandler
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
            consumer.brokerReplicaSet,
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

    // TODO TEZOS not supported yet
    /*
    @Bean
    fun tezosCollectionEventHandler(handler: IncomingEventHandler<CollectionEventDto>): TezosCollectionEventHandler {
        return TezosCollectionEventHandler(handler)
    }
     */

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
        handler: BlockchainEventHandler<ItemEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<ItemEventDto> {
        val consumer = factory.createItemConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    // TODO: Tezos will support events on collections => create a worker here.

    @Bean
    fun tezosOwnershipWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<OwnershipEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<OwnershipEventDto> {
        val consumer = factory.createOwnershipConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosOrderWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): KafkaConsumerWorker<OrderEventDto> {
        val consumer = factory.createOrderConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosActivityWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<ActivityDto, com.rarible.protocol.union.dto.ActivityDto>
    ): KafkaConsumerWorker<ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}