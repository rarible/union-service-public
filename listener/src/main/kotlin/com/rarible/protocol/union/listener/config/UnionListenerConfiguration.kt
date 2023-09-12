package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.core.model.ItemEventDelayMetric
import com.rarible.protocol.union.core.model.OrderEventDelayMetric
import com.rarible.protocol.union.core.model.OwnershipEventDelayMetric
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import com.rarible.protocol.union.listener.downloader.DownloadTaskRouter
import com.rarible.protocol.union.listener.downloader.KafkaMetaTaskRouter
import com.rarible.protocol.union.listener.downloader.MongoMetaTaskRouter
import com.rarible.protocol.union.listener.handler.MetricsInternalEventHandlerFactory
import com.rarible.protocol.union.listener.handler.internal.CollectionMetaTaskSchedulerHandler
import com.rarible.protocol.union.listener.handler.internal.ItemMetaTaskSchedulerHandler
import com.rarible.protocol.union.listener.handler.internal.UnionInternalChunkedEventHandler
import com.rarible.protocol.union.listener.handler.internal.UnionInternalEventChunker
import com.rarible.protocol.union.listener.handler.internal.UnionInternalEventHandler
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
@Import(value = [EnrichmentConsumerConfiguration::class, SearchConfiguration::class])
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    private val downloadTaskService: DownloadTaskService,
    private val listenerProperties: UnionListenerProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val meterRegistry: MeterRegistry,
    private val ff: FeatureFlagsProperties,
    private val handlerWrapperFactory: MetricsInternalEventHandlerFactory,
    private val handler: UnionInternalEventHandler,
    private val chunker: UnionInternalEventChunker,
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val kafkaConsumerFactory = RaribleKafkaConsumerFactory(
        env = env,
        host = host,
    )
    private val properties = listenerProperties.consumer

    @Bean
    fun ethereumUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.ETHEREUM,
        )
    }

    @Bean
    fun ethereumUnionBlockchainEventContainer(
        ethereumUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.ETHEREUM,
            unionBlockchainEventContainerFactory = ethereumUnionBlockchainEventContainerFactory,
        )
    }

    @Bean
    fun polygonUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.POLYGON,
        )
    }

    @Bean
    fun polygonUnionBlockchainEventContainer(
        polygonUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.POLYGON,
            unionBlockchainEventContainerFactory = polygonUnionBlockchainEventContainerFactory,
        )
    }

    @Bean
    fun mantleUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.MANTLE,
        )
    }

    @Bean
    fun mantleUnionBlockchainEventContainer(
        mantleUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.MANTLE,
            unionBlockchainEventContainerFactory = mantleUnionBlockchainEventContainerFactory,
        )
    }

    @Bean
    fun immutablexUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.IMMUTABLEX,
        )
    }

    @Bean
    fun immutablexUnionBlockchainEventContainer(
        immutablexUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.IMMUTABLEX,
            unionBlockchainEventContainerFactory = immutablexUnionBlockchainEventContainerFactory,
        )
    }

    @Bean
    fun flowUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.FLOW,
        )
    }

    @Bean
    fun flowUnionBlockchainEventContainer(
        flowUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.FLOW,
            unionBlockchainEventContainerFactory = flowUnionBlockchainEventContainerFactory,
        )
    }

    @Bean
    fun tezosUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.TEZOS,
        )
    }

    @Bean
    fun tezosUnionBlockchainEventContainer(
        tezosUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.TEZOS,
            unionBlockchainEventContainerFactory = tezosUnionBlockchainEventContainerFactory,
        )
    }

    @Bean
    fun solanaUnionBlockchainEventContainerFactory(): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainerFactory(
            blockchain = BlockchainDto.SOLANA,
        )
    }

    @Bean
    fun solanaUnionBlockchainEventContainer(
        solanaUnionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        return unionBlockchainEventContainer(
            blockchain = BlockchainDto.SOLANA,
            unionBlockchainEventContainerFactory = solanaUnionBlockchainEventContainerFactory,
        )
    }

    private fun unionBlockchainEventContainerFactory(
        blockchain: BlockchainDto,
    ): RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent> {
        val workers = properties.getWorkerProperties(blockchain)
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = properties.brokerReplicaSet,
                valueClass = UnionInternalBlockchainEvent::class.java,
                concurrency = workers.concurrency,
                batchSize = workers.batchSize,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    private fun unionBlockchainEventContainer(
        blockchain: BlockchainDto,
        unionBlockchainEventContainerFactory: RaribleKafkaListenerContainerFactory<UnionInternalBlockchainEvent>,
    ): ConcurrentMessageListenerContainer<String, UnionInternalBlockchainEvent> {
        val workers = properties.getWorkerProperties(blockchain)
        val settings = RaribleKafkaConsumerSettings(
            topic = UnionInternalTopicProvider.getInternalBlockchainTopic(env, blockchain),
            group = consumerGroup("blockchain.${blockchain.name.lowercase()}"),
            coroutineThreadCount = workers.coroutineThreadCount,
            // Mandatory to be true with InternalBatchEventHandler, do not set it FALSE!
            // Sync handling won't work with UnionInternalEventChunker, can cause bugs
            async = true,
        )
        val chunkedHandler = UnionInternalChunkedEventHandler(handler, chunker, blockchain)
        return if (ff.enableInternalEventChunkAsyncHandling) {
            kafkaConsumerFactory.createWorker(
                settings,
                handlerWrapperFactory.create(chunkedHandler as InternalBatchEventHandler<UnionInternalBlockchainEvent>),
                unionBlockchainEventContainerFactory
            )
        } else {
            kafkaConsumerFactory.createWorker(
                settings,
                handlerWrapperFactory.create(chunkedHandler as InternalEventHandler<UnionInternalBlockchainEvent>),
                unionBlockchainEventContainerFactory,
            )
        }
    }

    @Bean
    fun unionReconciliationMarkEventWorker(
        handler: InternalEventHandler<ReconciliationMarkEvent>,
        unionReconciliationMarkEventContainerFactory: RaribleKafkaListenerContainerFactory<ReconciliationMarkEvent>,
    ): ConcurrentMessageListenerContainer<String, ReconciliationMarkEvent> {
        val settings = RaribleKafkaConsumerSettings(
            topic = UnionInternalTopicProvider.getReconciliationMarkTopic(env),
            group = consumerGroup("reconciliation"),
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, unionReconciliationMarkEventContainerFactory)
    }

    @Bean
    fun unionReconciliationMarkEventContainerFactory(): RaribleKafkaListenerContainerFactory<ReconciliationMarkEvent> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = properties.brokerReplicaSet,
                valueClass = ReconciliationMarkEvent::class.java,
                concurrency = 1,
                batchSize = 50,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    // --------------- Meta 3.0 beans START
    @Bean
    fun itemDownloadTaskEventContainerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> {
        val itemProperties = listenerProperties.metaScheduling.item
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = properties.brokerReplicaSet,
                valueClass = DownloadTaskEvent::class.java,
                concurrency = itemProperties.workers,
                batchSize = itemProperties.batchSize,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun itemMetaDownloadScheduleWorker(
        handler: ItemMetaTaskSchedulerHandler,
        itemDownloadTaskEventContainerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val consumerGroupSuffix = "meta.item"
        val settings = RaribleKafkaConsumerSettings(
            topic = UnionInternalTopicProvider.getItemMetaDownloadTaskSchedulerTopic(env),
            group = consumerGroup("download.scheduler.$consumerGroupSuffix"),
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, itemDownloadTaskEventContainerFactory)
    }

    @Bean
    @Qualifier("item.meta.schedule.router")
    fun itemMetaTaskRouter(): DownloadTaskRouter {
        if (ff.enableMetaMongoPipeline) {
            return MongoMetaTaskRouter("item", downloadTaskService)
        }
        val producers = HashMap<String, RaribleKafkaProducer<DownloadTaskEvent>>()
        ItemMetaPipeline.values().map { it.pipeline }.forEach { pipeline ->
            val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline)
            val producer = RaribleKafkaProducer(
                clientId = "$env.protocol-union-service.meta.scheduler",
                valueSerializerClass = UnionKafkaJsonSerializer::class.java,
                valueClass = DownloadTaskEvent::class.java,
                defaultTopic = topic,
                bootstrapServers = properties.brokerReplicaSet,
                compression = properties.compression,
            )
            producers[pipeline] = producer
        }
        return KafkaMetaTaskRouter(producers)
    }

    @Bean
    fun collectionDownloadTaskEventContainerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> {
        val collectionProperties = listenerProperties.metaScheduling.collection
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = properties.brokerReplicaSet,
                valueClass = DownloadTaskEvent::class.java,
                concurrency = collectionProperties.workers,
                batchSize = collectionProperties.batchSize,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun collectionMetaDownloadScheduleWorker(
        handler: CollectionMetaTaskSchedulerHandler,
        collectionDownloadTaskEventContainerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val consumerGroupSuffix = "meta.collection"

        val settings = RaribleKafkaConsumerSettings(
            topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskSchedulerTopic(env),
            group = consumerGroup("download.scheduler.$consumerGroupSuffix"),
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, collectionDownloadTaskEventContainerFactory)
    }

    @Bean
    @Qualifier("collection.meta.schedule.router")
    fun collectionMetaTaskRouter(): DownloadTaskRouter {
        if (ff.enableMetaMongoPipeline) {
            return MongoMetaTaskRouter("collection", downloadTaskService)
        }
        val producers = HashMap<String, RaribleKafkaProducer<DownloadTaskEvent>>()
        CollectionMetaPipeline.values().map { it.pipeline }.forEach { pipeline ->
            val topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskExecutorTopic(env, pipeline)
            val producer = RaribleKafkaProducer(
                clientId = "$env.protocol-union-service.meta.scheduler",
                valueSerializerClass = UnionKafkaJsonSerializer::class.java,
                valueClass = DownloadTaskEvent::class.java,
                defaultTopic = topic,
                bootstrapServers = properties.brokerReplicaSet,
                compression = properties.compression,
            )
            producers[pipeline] = producer
        }
        return KafkaMetaTaskRouter(producers)
    }

    // --------------- Meta 3.0 beans END

    private fun consumerGroup(suffix: String): String {
        return "protocol.union.$suffix"
    }

    @Bean
    fun itemCompositeRegisteredTimer(): CompositeRegisteredTimer {
        return ItemEventDelayMetric(listenerProperties.metrics.rootPath).bind(meterRegistry)
    }

    @Bean
    fun ownershipCompositeRegisteredTimer(): CompositeRegisteredTimer {
        return OwnershipEventDelayMetric(listenerProperties.metrics.rootPath).bind(meterRegistry)
    }

    @Bean
    fun orderCompositeRegisteredTimer(): CompositeRegisteredTimer {
        return OrderEventDelayMetric(listenerProperties.metrics.rootPath).bind(meterRegistry)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(UnionListenerConfiguration::class.java)
    }
}
