package com.rarible.protocol.union.meta.loader.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.handler.BatchedConsumerWorker
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaNotifier
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorHandler
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorManager
import com.rarible.protocol.union.meta.loader.executor.DownloadMetrics
import com.rarible.protocol.union.meta.loader.executor.DownloadPool
import com.rarible.protocol.union.meta.loader.executor.ItemDownloadExecutor
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

@Configuration
@ConditionalOnProperty("common.feature-flags.enableMetaPipeline", havingValue = "true", matchIfMissing = false)
@Import(value = [UnionMetaLoaderConfiguration::class])
@EnableConfigurationProperties(value = [UnionMetaLoaderProperties::class])
class DownloadExecutorConfiguration(
    private val metaProperties: UnionMetaProperties,
    private val metaLoaderProperties: UnionMetaLoaderProperties,
    private val meterRegistry: MeterRegistry,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val logger by Logger()

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val clientIdPrefix = "$env.$host.${UUID.randomUUID()}"

    @Bean
    @Qualifier("item.meta.download.executor.manager")
    fun itemMetaDownloadExecutorManager(
        itemMetaRepository: ItemMetaRepository,
        itemMetaDownloader: ItemMetaDownloader,
        itemMetaNotifier: ItemMetaNotifier,
        metrics: DownloadMetrics
    ): DownloadExecutorManager {
        val maxRetries = metaProperties.retryIntervals.size
        val executors = HashMap<String, DownloadExecutor<UnionMeta>>()
        ItemMetaPipeline.values().map { it.name.lowercase() }.forEach { pipeline ->
            val conf = getItemPipelineConfiguration(pipeline)
            val pool = DownloadPool(conf.poolSize, "item-meta-task-executor")
            val executor = ItemDownloadExecutor(
                itemMetaRepository,
                itemMetaDownloader,
                itemMetaNotifier,
                pool,
                metrics,
                maxRetries
            )
            executors[pipeline] = executor
            logger.info(
                "Created item-meta-task-executor (pipeline: $pipeline, poolSize: ${conf.poolSize})"
            )
        }
        return DownloadExecutorManager(executors)
    }

    @Bean
    fun itemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager
    ): BatchedConsumerWorker<DownloadTask> {
        val type = "item"
        val workers = ItemMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            val conf = getItemPipelineConfiguration(pipeline)
            val handler = DownloadExecutorHandler(pipeline, executorManager)
            createMetaDownloaderBatchConsumer(pipeline, conf.workers, conf.batchSize, type, handler)
        }.map { it.workers }.flatten()

        // Just join all pipeline workers into single list
        return BatchedConsumerWorker(workers)
    }

    private fun getItemPipelineConfiguration(pipeline: String): ExecutorPipelineProperties {
        return metaLoaderProperties.downloader.item[pipeline]
            ?: throw IllegalArgumentException(
                "Properties for Item download executor of pipeline $pipeline are not specified"
            )
    }

    private fun createMetaDownloaderBatchConsumer(
        pipeline: String,
        workers: Int,
        batchSize: Int,
        type: String,
        handler: DownloadExecutorHandler
    ): BatchedConsumerWorker<UnionMeta> {
        val consumerGroupSuffix = "meta.$type"
        val clientIdSuffix = "$type-meta-task-executor"
        val workerSet = (1..workers).map {
            ConsumerBatchWorker(
                consumer = createDownloadExecutorConsumer(it, clientIdSuffix, consumerGroupSuffix, pipeline),
                properties = DaemonWorkerProperties(consumerBatchSize = batchSize),
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "internal-$type-meta-task-executor-$it"
            )
        }
        logger.info(
            "Created $workers consumers for $type-download-executor (pipeline: $pipeline, batchSize: $batchSize)"
        )
        return BatchedConsumerWorker(workerSet)
    }

    private fun createDownloadExecutorConsumer(
        index: Int,
        clientIdSuffix: String,
        consumerGroupSuffix: String,
        pipeline: String
    ): RaribleKafkaConsumer<DownloadTask> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-$clientIdSuffix.$pipeline-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = DownloadTask::class.java,
            consumerGroup = consumerGroup("download.executor.$consumerGroupSuffix"),
            defaultTopic = UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline),
            bootstrapServers = metaLoaderProperties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }

}