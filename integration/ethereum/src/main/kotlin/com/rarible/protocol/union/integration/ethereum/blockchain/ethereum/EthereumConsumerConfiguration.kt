package com.rarible.protocol.union.integration.ethereum.blockchain.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.AuctionEventDto
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
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.event.EthActivityEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthAuctionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthCollectionEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthItemMetaEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOrderEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthOwnershipEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@EthereumConfiguration
@Import(EthereumApiConfiguration::class)
class EthereumConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: EthereumIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    private val consumer = properties.consumer!!

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    private val blockchain = BlockchainDto.ETHEREUM
    private val blockchainName = blockchain.name.lowercase()

    // -------------------- Handlers -------------------//

    @Bean
    @Qualifier("ethereum.item.handler")
    fun ethereumItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthItemEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("ethereum.itemMeta.handler")
    fun ethereumItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): EthItemMetaEventHandler {
        return EthItemMetaEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("ethereum.ownership.handler")
    fun ethereumOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): EthOwnershipEventHandler {
        return EthOwnershipEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("ethereum.collection.handler")
    fun ethereumCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): EthCollectionEventHandler {
        return EthCollectionEventHandler(blockchain, handler)
    }

    @Bean
    @Qualifier("ethereum.order.handler")
    fun ethereumOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: EthOrderConverter
    ): EthOrderEventHandler {
        return EthOrderEventHandler(blockchain, handler, converter)
    }

    @Bean
    @Qualifier("ethereum.auction.handler")
    fun ethereumAuctionEventHandler(
        handler: IncomingEventHandler<UnionAuctionEvent>,
        converter: EthAuctionConverter
    ): EthAuctionEventHandler {
        return EthAuctionEventHandler(blockchain, handler, converter)
    }

    @Bean
    @Qualifier("ethereum.activity.handler")
    fun ethereumActivityEventHandler(
        handler: IncomingEventHandler<UnionActivity>,
        converter: EthActivityConverter
    ): EthActivityEventHandler {
        return EthActivityEventHandler(blockchain, handler, converter)
    }

    // -------------------- Workers --------------------//

    @Bean
    fun ethereumItemWorker(
        @Qualifier("ethereum.item.handler") handler: BlockchainEventHandler<NftItemEventDto, UnionItemEvent>
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftItemEventDto::class.java,
        )
    }

    @Bean
    fun ethereumItemMetaWorker(
        @Qualifier("ethereum.itemMeta.handler") handler: BlockchainEventHandler<NftItemMetaEventDto, UnionItemMetaEvent>
    ): RaribleKafkaConsumerWorker<NftItemMetaEventDto> {
        return createConsumer(
            topic = NftItemEventTopicProvider.getItemMetaTopic(env, blockchainName),
            handler = handler,
            valueClass = NftItemMetaEventDto::class.java,
        )
    }

    @Bean
    fun ethereumOwnershipWorker(
        @Qualifier("ethereum.ownership.handler") handler: BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        return createConsumer(
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftOwnershipEventDto::class.java,
        )
    }

    @Bean
    fun ethereumCollectionWorker(
        @Qualifier("ethereum.collection.handler") handler: BlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        return createConsumer(
            topic = NftCollectionEventTopicProvider.getTopic(env, blockchainName),
            handler = handler,
            valueClass = NftCollectionEventDto::class.java,
        )
    }

    @Bean
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.handler") handler: BlockchainEventHandler<OrderEventDto, UnionOrderEvent>
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, blockchainName),
            handler = handler,
            valueClass = OrderEventDto::class.java,
        )
    }

    @Bean
    fun ethereumAuctionWorker(
        @Qualifier("ethereum.auction.handler") handler: BlockchainEventHandler<AuctionEventDto, UnionAuctionEvent>
    ): RaribleKafkaConsumerWorker<AuctionEventDto> {
        return createConsumer(
            topic = OrderIndexerTopicProvider.getAuctionUpdateTopic(env, blockchainName),
            handler = handler,
            valueClass = AuctionEventDto::class.java,
        )
    }

    @Bean
    fun ethereumActivityWorker(
        @Qualifier("ethereum.activity.handler") handler: BlockchainEventHandler<EthActivityEventDto, UnionActivity>
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
