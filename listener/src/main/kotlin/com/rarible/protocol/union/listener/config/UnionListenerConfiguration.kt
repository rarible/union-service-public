package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.event.UnionWrappedTopicProvider
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

@Configuration
@EnableRaribleTask
@EnableMongock
@Import(value = [EnrichmentConsumerConfiguration::class])
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    private val listenerProperties: UnionListenerProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val clientIdPrefix = "$env.$host.${UUID.randomUUID()}"

    @Bean
    fun unionWrappedEventConsumer(): RaribleKafkaConsumer<UnionWrappedEvent> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-wrapped-event-consumer",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionWrappedEvent::class.java,
            consumerGroup = consumerGroup("wrapped"),
            defaultTopic = UnionWrappedTopicProvider.getWrappedTopic(env),
            bootstrapServers = listenerProperties.consumer.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun unionWrappedEventWorker(
        consumer: RaribleKafkaConsumer<UnionWrappedEvent>,
        handler: InternalEventHandler<UnionWrappedEvent>
    ): KafkaConsumerWorker<UnionWrappedEvent> {
        return consumerFactory.createWrappedEventConsumer(
            consumer = consumer,
            handler = handler,
            daemon = listenerProperties.monitoringWorker,
            workers = listenerProperties.consumer.workers
        )
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }

}
