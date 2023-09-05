package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumerWorkerGroup
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
import com.rarible.protocol.union.listener.downloader.MetaTaskRouter
import com.rarible.protocol.union.listener.handler.MetricsInternalEventHandlerFactory
import com.rarible.protocol.union.listener.handler.internal.CollectionMetaTaskSchedulerHandler
import com.rarible.protocol.union.listener.handler.internal.ItemMetaTaskSchedulerHandler
import com.rarible.protocol.union.listener.handler.internal.UnionInternalChunkedEventHandler
import com.rarible.protocol.union.listener.handler.internal.UnionInternalEventChunker
import com.rarible.protocol.union.listener.handler.internal.UnionInternalEventHandler
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(value = [EnrichmentConsumerConfiguration::class, SearchConfiguration::class])
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    private val listenerProperties: UnionListenerProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val meterRegistry: MeterRegistry,
    activeBlockchains: List<BlockchainDto>,
    private val ff: FeatureFlagsProperties
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val kafkaConsumerFactory = RaribleKafkaConsumerFactory(
        env = env,
        host = host,
        deserializer = UnionKafkaJsonDeserializer::class.java
    )
    private val blockchains = activeBlockchains.toSet()
    private val properties = listenerProperties.consumer

    @Bean
    fun unionBlockchainEventWorker(
        handlerWrapperFactory: MetricsInternalEventHandlerFactory,
        handler: UnionInternalEventHandler,
        chunker: UnionInternalEventChunker
    ): RaribleKafkaConsumerWorker<UnionInternalBlockchainEvent> {
        val consumers = blockchains.map { blockchain ->
            val workers = properties.getWorkerProperties(blockchain)
            val settings = RaribleKafkaConsumerSettings(
                hosts = properties.brokerReplicaSet,
                topic = UnionInternalTopicProvider.getInternalBlockchainTopic(env, blockchain),
                group = consumerGroup("blockchain.${blockchain.name.lowercase()}"),
                concurrency = workers.concurrency,
                batchSize = workers.batchSize,
                // Mandatory to be true with InternalBatchEventHandler, do not set it FALSE!
                // Sync handling won't work with UnionInternalEventChunker, can cause bugs
                async = true,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                valueClass = UnionInternalBlockchainEvent::class.java
            )
            val chunkedHandler = UnionInternalChunkedEventHandler(handler, chunker, blockchain)
            if (ff.enableInternalEventChunkAsyncHandling) {
                kafkaConsumerFactory.createWorker(
                    settings,
                    handlerWrapperFactory.create(chunkedHandler as InternalBatchEventHandler<UnionInternalBlockchainEvent>)
                )
            } else {
                kafkaConsumerFactory.createWorker(
                    settings,
                    handlerWrapperFactory.create(chunkedHandler as InternalEventHandler<UnionInternalBlockchainEvent>)
                )
            }
        }
        return RaribleKafkaConsumerWorkerGroup(consumers)
    }

    @Bean
    fun unionReconciliationMarkEventWorker(
        handler: InternalEventHandler<ReconciliationMarkEvent>
    ): RaribleKafkaConsumerWorker<ReconciliationMarkEvent> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.brokerReplicaSet,
            topic = UnionInternalTopicProvider.getReconciliationMarkTopic(env),
            group = consumerGroup("reconciliation"),
            concurrency = 1,
            batchSize = 50,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = ReconciliationMarkEvent::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    // --------------- Meta 3.0 beans START
    @Bean
    fun itemMetaDownloadScheduleWorker(
        handler: ItemMetaTaskSchedulerHandler
    ): RaribleKafkaConsumerWorker<DownloadTaskEvent> {
        val consumerGroupSuffix = "meta.item"
        val itemProperties = listenerProperties.metaScheduling.item
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.brokerReplicaSet,
            topic = UnionInternalTopicProvider.getItemMetaDownloadTaskSchedulerTopic(env),
            group = consumerGroup("download.scheduler.$consumerGroupSuffix"),
            concurrency = itemProperties.workers,
            batchSize = itemProperties.batchSize,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = DownloadTaskEvent::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    @Qualifier("item.meta.schedule.router")
    fun itemMetaTaskRouter(): MetaTaskRouter {
        val producers = HashMap<String, RaribleKafkaProducer<DownloadTaskEvent>>()
        ItemMetaPipeline.values().map { it.pipeline }.forEach { pipeline ->
            val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline)
            val producer = RaribleKafkaProducer(
                clientId = "$env.protocol-union-service.meta.scheduler",
                valueSerializerClass = UnionKafkaJsonSerializer::class.java,
                valueClass = DownloadTaskEvent::class.java,
                defaultTopic = topic,
                bootstrapServers = properties.brokerReplicaSet
            )
            producers[pipeline] = producer
        }
        return MetaTaskRouter(producers)
    }

    @Bean
    fun collectionMetaDownloadScheduleWorker(
        handler: CollectionMetaTaskSchedulerHandler
    ): RaribleKafkaConsumerWorker<DownloadTaskEvent> {
        val collectionProperties = listenerProperties.metaScheduling.collection
        val consumerGroupSuffix = "meta.collection"

        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.brokerReplicaSet,
            topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskSchedulerTopic(env),
            group = consumerGroup("download.scheduler.$consumerGroupSuffix"),
            concurrency = collectionProperties.workers,
            batchSize = collectionProperties.batchSize,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = DownloadTaskEvent::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    @Qualifier("collection.meta.schedule.router")
    fun collectionMetaTaskRouter(): MetaTaskRouter {
        val producers = HashMap<String, RaribleKafkaProducer<DownloadTaskEvent>>()
        CollectionMetaPipeline.values().map { it.pipeline }.forEach { pipeline ->
            val topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskExecutorTopic(env, pipeline)
            val producer = RaribleKafkaProducer(
                clientId = "$env.protocol-union-service.meta.scheduler",
                valueSerializerClass = UnionKafkaJsonSerializer::class.java,
                valueClass = DownloadTaskEvent::class.java,
                defaultTopic = topic,
                bootstrapServers = properties.brokerReplicaSet
            )
            producers[pipeline] = producer
        }
        return MetaTaskRouter(producers)
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
