package com.rarible.protocol.union.integration.ethereum

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumerWorkerGroup
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(EthApiConfiguration::class)
class EthConsumerConfiguration(
    private val itemHandler: IncomingEventHandler<UnionItemEvent>,
    private val itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
    private val ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
    private val collectionHandler: IncomingEventHandler<UnionCollectionEvent>,
    private val orderHandler: IncomingEventHandler<UnionOrderEvent>,
    private val auctionHandler: IncomingEventHandler<UnionAuctionEvent>,
    private val activityHandler: IncomingEventHandler<UnionActivity>,

    private val consumerFactory: ConsumerFactory,
    private val properties: EthIntegrationProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
) {

    private val env = applicationEnvironmentInfo.name

    @Bean
    fun ethereumItemWorker(): RaribleKafkaConsumerWorker<NftItemEventDto> {
        return createConsumer(
            topic = { NftItemEventTopicProvider.getTopic(env, it.name.lowercase()) },
            handler = { EthItemEventHandler(it, itemHandler) },
            valueClass = NftItemEventDto::class.java,
        )
    }

    @Bean
    fun ethereumItemMetaWorker(): RaribleKafkaConsumerWorker<NftItemMetaEventDto> {
        return createConsumer(
            topic = { NftItemEventTopicProvider.getItemMetaTopic(env, it.name.lowercase()) },
            handler = { EthItemMetaEventHandler(it, itemMetaHandler) },
            valueClass = NftItemMetaEventDto::class.java,
        )
    }

    @Bean
    fun ethereumOwnershipWorker(): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        return createConsumer(
            topic = { NftOwnershipEventTopicProvider.getTopic(env, it.name.lowercase()) },
            handler = { EthOwnershipEventHandler(it, ownershipHandler) },
            valueClass = NftOwnershipEventDto::class.java,
        )
    }

    @Bean
    fun ethereumCollectionWorker(): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        return createConsumer(
            topic = { NftCollectionEventTopicProvider.getTopic(env, it.name.lowercase()) },
            handler = { EthCollectionEventHandler(it, collectionHandler) },
            valueClass = NftCollectionEventDto::class.java,
        )
    }

    @Bean
    fun ethereumOrderWorker(
        ethOrderConverter: EthOrderConverter
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return createConsumer(
            topic = { OrderIndexerTopicProvider.getOrderUpdateTopic(env, it.name.lowercase()) },
            handler = { EthOrderEventHandler(it, orderHandler, ethOrderConverter) },
            valueClass = OrderEventDto::class.java,
        )
    }

    @Bean
    fun ethereumAuctionWorker(
        ethAuctionConverter: EthAuctionConverter
    ): RaribleKafkaConsumerWorker<AuctionEventDto> {
        return createConsumer(
            topic = { OrderIndexerTopicProvider.getAuctionUpdateTopic(env, it.name.lowercase()) },
            handler = { EthAuctionEventHandler(it, auctionHandler, ethAuctionConverter) },
            valueClass = AuctionEventDto::class.java,
        )
    }

    @Bean
    fun ethereumActivityWorker(
        ethActivityConverter: EthActivityConverter
    ): RaribleKafkaConsumerWorker<EthActivityEventDto> {
        return createConsumer(
            topic = { ActivityTopicProvider.getActivityTopic(env, it.name.lowercase()) },
            handler = { EthActivityEventHandler(it, activityHandler, ethActivityConverter) },
            valueClass = EthActivityEventDto::class.java,
        )
    }

    private fun <B, U> createConsumer(
        valueClass: Class<B>,
        topic: (blockchain: BlockchainDto) -> String,
        handler: (blockchain: BlockchainDto) -> BlockchainEventHandler<B, U>
    ): RaribleKafkaConsumerWorkerGroup<B> {
        val workers = properties.active.map { blockchain ->
            val consumer = properties.blockchains[blockchain]!!.consumer!!
            consumerFactory.createBlockchainConsumerWorker(
                hosts = consumer.brokerReplicaSet!!,
                topic = topic(blockchain),
                handler = handler(blockchain),
                valueClass = valueClass,
                workers = consumer.workers,
                batchSize = consumer.batchSize
            )
        }
        return RaribleKafkaConsumerWorkerGroup(workers)
    }
}
