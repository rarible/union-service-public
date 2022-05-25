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
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupConsumerConfiguration
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupDummyApiConfiguration
import com.rarible.protocol.union.integration.tezos.event.TezosActivityEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosCollectionEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosItemEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosOrderEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosOwnershipEventHandler
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@TezosConfiguration
@Import(value = [TezosApiConfiguration::class, DipDupConsumerConfiguration::class])
class TezosConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: TezosIntegrationProperties,
    @Value("\${rarible.core.client.k8s:false}")
    private val k8s: Boolean,
    @Value("\${integration.tezos.dipdup.enable:false}")
    private val isDipDupEnabled: Boolean,
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

    @ConditionalOnProperty(prefix = "integration.tezos.dipdup", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    @Bean
    fun tezosItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): TezosItemEventHandler {
        return TezosItemEventHandler(handler)
    }

    @ConditionalOnProperty(prefix = "integration.tezos.dipdup", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    @Bean
    fun tezosOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): TezosOwnershipEventHandler {
        return TezosOwnershipEventHandler(handler)
    }

    @ConditionalOnProperty(prefix = "integration.tezos.dipdup", name = ["enabled"], havingValue = "false", matchIfMissing = true)
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
        return TezosActivityEventHandler(handler, converter, isDipDupEnabled)
    }

    //-------------------- Workers --------------------//

    @ConditionalOnProperty(prefix = "integration.tezos.dipdup", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    @Bean
    fun tezosItemWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosItemSafeEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<TezosItemSafeEventDto> {
        val consumer = factory.createItemConsumer(consumerGroup(consumerFactory.itemGroup))
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @ConditionalOnProperty(prefix = "integration.tezos.dipdup", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    @Bean
    fun tezosCollectionWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosCollectionSafeEventDto, UnionCollectionEvent>
    ): KafkaConsumerWorker<TezosCollectionSafeEventDto> {
        val consumer = factory.createCollectionConsumer(consumerGroup(consumerFactory.itemGroup))
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @ConditionalOnProperty(prefix = "integration.tezos.dipdup", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    @Bean
    fun tezosOwnershipWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosOwnershipSafeEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<TezosOwnershipSafeEventDto> {
        val consumer = factory.createOwnershipConsumer(consumerGroup(consumerFactory.ownershipGroup))
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosOrderWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosOrderSafeEventDto, UnionOrderEvent>
    ): KafkaConsumerWorker<TezosOrderSafeEventDto> {
        val consumer = factory.createOrderConsumer(consumerGroup(consumerFactory.orderGroup))
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosActivityWorker(
        factory: TezosEventsConsumerFactory,
        handler: BlockchainEventHandler<TezosActivitySafeDto, com.rarible.protocol.union.dto.ActivityDto>
    ): KafkaConsumerWorker<TezosActivitySafeDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(consumerFactory.activityGroup))
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }

    private fun consumerGroup(group: String): String {
        // Since Tezos has kafka outside our cluster and several envs uses same topics, groupId should be customized
        return if (k8s) {
            "k8s.$group"
        } else {
            group
        }
    }
}
