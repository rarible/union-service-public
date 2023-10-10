package com.rarible.protocol.union.integration.ethereum.blockchain.polygon

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
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
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.event.EthActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemMetaEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@PolygonConfiguration
@Import(PolygonApiConfiguration::class)
class PolygonConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: PolygonIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    private val blockchain = BlockchainDto.POLYGON
    private val blockchainName = blockchain.name.lowercase()

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("polygon.item.handler")
    fun polygonItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthItemEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("polygon.itemMeta.handler")
    fun polygonItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): EthItemMetaEventHandler {
        return EthItemMetaEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("polygon.ownership.handler")
    fun polygonOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return EthOwnershipEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("polygon.collection.handler")
    fun polygonCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return EthCollectionEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("polygon.order.handler")
    fun polygonOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return EthOrderEventHandler(blockchain, handler, converter)
    }

    @Bean
    @Qualifier("polygon.activity.handler")
    fun polygonActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return EthActivityEventHandler(blockchain, handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun polygonItemWorker(
        @Qualifier("polygon.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftItemEventDto::class.java,
        )
    }

    @Bean
    fun polygonItemMetaWorker(
        @Qualifier("polygon.itemMeta.handler") handler: BlockchainEventHandler<NftItemMetaEventDto, UnionItemMetaEvent>
    ): RaribleKafkaConsumerWorker<NftItemMetaEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getItemMetaTopic(env, blockchainName),
            handler = handler,
            valueClass = NftItemMetaEventDto::class.java,
        )
    }

    @Bean
    fun polygonOwnershipWorker(
        @Qualifier("polygon.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        return createConsumer(
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftOwnershipEventDto::class.java,
        )
    }

    @Bean
    fun polygonCollectionWorker(
        @Qualifier("polygon.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        return createConsumer(
            topic = NftCollectionEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftCollectionEventDto::class.java,
        )
    }

    @Bean
    fun polygonOrderWorker(
        @Qualifier("polygon.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, blockchainName),
            handler = handler,
            valueClass = OrderEventDto::class.java,
        )
    }

    @Bean
    fun polygonActivityWorker(
        @Qualifier("polygon.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>
    ): RaribleKafkaConsumerWorker<EthActivityEventDto> {
        return createConsumer(
            topic = ActivityTopicProvider.getActivityTopic(env, blockchainName),
            handler = handler,
            valueClass = EthActivityEventDto::class.java,
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
