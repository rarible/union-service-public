package com.rarible.protocol.union.integration.ethereum.blockchain.mantle

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider
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
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.event.EthActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemMetaEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleItemMetaEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.MantleOwnershipEventHandler
import com.rarible.protocol.union.integration.ethereum.event.PolygonItemMetaEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@MantleConfiguration
@Import(MantleApiConfiguration::class)
class MantleConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: MantleIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    private val blockchain = Blockchain.MANTLE

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("mantle.item.handler")
    fun mantleItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return MantleItemEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.itemMeta.handler")
    fun mantleItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): EthItemMetaEventHandler {
        return MantleItemMetaEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.ownership.handler")
    fun mantleOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return MantleOwnershipEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.collection.handler")
    fun mantleCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return MantleCollectionEventHandler(handler)
    }

    @Bean
    @Qualifier("mantle.order.handler")
    fun mantleOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return MantleOrderEventHandler(handler, converter)
    }

    @Bean
    @Qualifier("mantle.activity.handler")
    fun mantleActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return MantleActivityEventHandler(handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun mantleItemWorker(
        @Qualifier("mantle.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftItemEventDto::class.java,
            eventType = EventType.ITEM,
        )
    }

    @Bean
    fun mantleItemMetaWorker(
        @Qualifier("mantle.itemMeta.handler") handler: BlockchainEventHandler<NftItemMetaEventDto, UnionItemMetaEvent>
    ): RaribleKafkaConsumerWorker<NftItemMetaEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getItemMetaTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftItemMetaEventDto::class.java,
            eventType = EventType.ITEM,
        )
    }

    @Bean
    fun mantleOwnershipWorker(
        @Qualifier("mantle.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        return createConsumer(
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftOwnershipEventDto::class.java,
            eventType = EventType.OWNERSHIP,
        )
    }

    @Bean
    fun mantleCollectionWorker(
        @Qualifier("mantle.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        return createConsumer(
            topic = NftCollectionEventTopicProvider.getTopic(env, blockchain.value),
            handler = handler,
            valueClass = NftCollectionEventDto::class.java,
            eventType = EventType.COLLECTION,
        )
    }

    @Bean
    fun mantleOrderWorker(
        @Qualifier("mantle.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, blockchain.value),
            handler = handler,
            valueClass = OrderEventDto::class.java,
            eventType = EventType.ORDER,
        )
    }

    @Bean
    fun mantleActivityWorker(
        @Qualifier("mantle.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>
    ): RaribleKafkaConsumerWorker<EthActivityEventDto> {
        return createConsumer(
            topic = ActivityTopicProvider.getActivityTopic(env, blockchain.value),
            handler = handler,
            valueClass = EthActivityEventDto::class.java,
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
