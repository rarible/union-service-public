package com.rarible.protocol.union.integration.ethereum.blockchain.mantle

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

    private val blockchain = BlockchainDto.MANTLE
    private val blockchainName = blockchain.name.lowercase()

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("mantle.item.handler")
    fun mantleItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthItemEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("mantle.itemMeta.handler")
    fun mantleItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): EthItemMetaEventHandler {
        return EthItemMetaEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("mantle.ownership.handler")
    fun mantleOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return EthOwnershipEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("mantle.collection.handler")
    fun mantleCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return EthCollectionEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("mantle.order.handler")
    fun mantleOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return EthOrderEventHandler(blockchain, handler, converter)
    }

    @Bean
    @Qualifier("mantle.activity.handler")
    fun mantleActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return EthActivityEventHandler(blockchain, handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun mantleItemWorker(
        @Qualifier("mantle.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftItemEventDto::class.java,
        )
    }

    @Bean
    fun mantleItemMetaWorker(
        @Qualifier("mantle.itemMeta.handler") handler: BlockchainEventHandler<NftItemMetaEventDto, UnionItemMetaEvent>
    ): RaribleKafkaConsumerWorker<NftItemMetaEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getItemMetaTopic(env, blockchainName),
            handler = handler,
            valueClass = NftItemMetaEventDto::class.java,
        )
    }

    @Bean
    fun mantleOwnershipWorker(
        @Qualifier("mantle.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        return createConsumer(
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftOwnershipEventDto::class.java,
        )
    }

    @Bean
    fun mantleCollectionWorker(
        @Qualifier("mantle.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        return createConsumer(
            topic = NftCollectionEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftCollectionEventDto::class.java,
        )
    }

    @Bean
    fun mantleOrderWorker(
        @Qualifier("mantle.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, blockchainName),
            handler = handler,
            valueClass = OrderEventDto::class.java,
        )
    }

    @Bean
    fun mantleActivityWorker(
        @Qualifier("mantle.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>
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
