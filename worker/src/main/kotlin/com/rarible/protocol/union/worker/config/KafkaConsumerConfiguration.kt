package com.rarible.protocol.union.worker.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.ProducerProperties
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.worker.kafka.LagService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaConsumerConfiguration(
    private val producerProperties: ProducerProperties
) {

    @Bean(destroyMethod = "close")
    fun kafkaConsumer(): KafkaConsumer<String, String> = KafkaConsumer(
        mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to producerProperties.brokerReplicaSet,
        )
    )

    @Bean
    fun lagService(
        kafkaConsumer: KafkaConsumer<String, String>,
        kafkaGroupFactory: KafkaGroupFactory,
        applicationEnvironmentInfo: ApplicationEnvironmentInfo,
        workerProperties: WorkerProperties,
    ): LagService = LagService(
        kafkaConsumer = kafkaConsumer,
        bootstrapServers = producerProperties.brokerReplicaSet,
        metaConsumerGroup = kafkaGroupFactory.metaDownloadExecutorGroup(KafkaGroupFactory.ITEM_TYPE),
        refreshTopic = UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(
            applicationEnvironmentInfo.name,
            ItemMetaPipeline.REFRESH.pipeline
        ),
        maxLag = workerProperties.collectionMetaRefresh.maxKafkaLag,
    )
}