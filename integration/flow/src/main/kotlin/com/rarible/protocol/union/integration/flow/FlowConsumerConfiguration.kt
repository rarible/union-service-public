package com.rarible.protocol.union.integration.flow

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.dto.FlowActivityEventDto
import com.rarible.protocol.dto.FlowActivityEventTopicProvider
import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.dto.FlowNftCollectionEventTopicProvider
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftOwnershipEventTopicProvider
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderEventTopicProvider
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.event.FlowActivityEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowCollectionEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowItemEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOrderEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOwnershipEventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@FlowConfiguration
@Import(FlowApiConfiguration::class)
class FlowConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: FlowIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    // -------------------- Handlers -------------------//

    @Bean
    fun flowItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): FlowItemEventHandler {
        return FlowItemEventHandler(handler)
    }

    @Bean
    fun flowOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): FlowOwnershipEventHandler {
        return FlowOwnershipEventHandler(handler)
    }

    @Bean
    fun flowCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): FlowCollectionEventHandler {
        return FlowCollectionEventHandler(handler)
    }

    @Bean
    fun flowOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: FlowOrderConverter
    ): FlowOrderEventHandler {
        return FlowOrderEventHandler(handler, converter)
    }

    @Bean
    fun flowActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: FlowActivityConverter
    ): FlowActivityEventHandler {
        return FlowActivityEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun flowItemWorker(
        handler: BlockchainEventHandler<FlowNftItemEventDto, UnionItemEvent>
    ): RaribleKafkaConsumerWorker<FlowNftItemEventDto> {
        return createConsumer(
            topic = FlowNftItemEventTopicProvider.getTopic(env),
            handler = handler,
            valueClass = FlowNftItemEventDto::class.java,
        )
    }

    @Bean
    fun flowOwnershipWorker(
        handler: BlockchainEventHandler<FlowOwnershipEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<FlowOwnershipEventDto> {
        return createConsumer(
            topic = FlowNftOwnershipEventTopicProvider.getTopic(env),
            handler = handler,
            valueClass = FlowOwnershipEventDto::class.java,
        )
    }

    @Bean
    fun flowCollectionWorker(
        handler: BlockchainEventHandler<FlowCollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<FlowCollectionEventDto> {
        return createConsumer(
            topic = FlowNftCollectionEventTopicProvider.getTopic(env),
            handler = handler,
            valueClass = FlowCollectionEventDto::class.java,
        )
    }

    @Bean
    fun flowOrderWorker(
        handler: BlockchainEventHandler<FlowOrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<FlowOrderEventDto> {
        return createConsumer(
            topic = FlowOrderEventTopicProvider.getTopic(env),
            handler = handler,
            valueClass = FlowOrderEventDto::class.java,
        )
    }

    @Bean
    fun flowActivityWorker(
        handler: BlockchainEventHandler<FlowActivityEventDto, UnionActivity>
    ): RaribleKafkaConsumerWorker<FlowActivityEventDto> {
        return createConsumer(
            topic = FlowActivityEventTopicProvider.getActivityTopic(env),
            handler = handler,
            valueClass = FlowActivityEventDto::class.java,
        )
    }

    private fun <B, U> createConsumer(
        topic: String,
        handler: BlockchainEventHandler<B, U>,
        valueClass: Class<B>,
    ): RaribleKafkaConsumerWorker<B> {
        return consumerFactory.createBlockchainConsumerWorkerGroup(
            hosts = consumer.brokerReplicaSet!!,
            topic = topic,
            handler = handler,
            valueClass = valueClass,
            workers = workers,
            batchSize = batchSize
        )
    }
}
