package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.ProducerProperties
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [ProducerProperties::class])
class EnrichmentProducerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: ProducerProperties
) {

    private val env = applicationEnvironmentInfo.name
    private val producerBrokerReplicaSet = properties.brokerReplicaSet

    @Bean
    fun reconciliationMarkEventProducer(): RaribleKafkaProducer<ReconciliationMarkEvent> {
        val topic = UnionInternalTopicProvider.getReconciliationMarkTopic(env)
        return createUnionProducer("reconciliation", topic, ReconciliationMarkEvent::class.java)
    }

    @Bean
    @Qualifier("download.scheduler.task.producer.item-meta")
    fun itemDownloadTaskProducer(): RaribleKafkaProducer<DownloadTaskEvent> {
        val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskSchedulerTopic(env)
        return createUnionProducer("meta.publisher", topic, DownloadTaskEvent::class.java)
    }

    @Bean
    @Qualifier("download.scheduler.task.producer.collection-meta")
    fun collectionDownloadTaskProducer(): RaribleKafkaProducer<DownloadTaskEvent> {
        val topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskSchedulerTopic(env)
        return createUnionProducer("meta.publisher", topic, DownloadTaskEvent::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "$env.protocol-union-service.$clientSuffix",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = producerBrokerReplicaSet
        )
    }
}
