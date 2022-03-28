package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.union.dto.ActivityDto
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class KafkaConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val kafkaProperties: KafkaProperties,
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        const val ACTIVITY = "activity"
    }

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    @Bean
    fun activityWorker(
        handler: ConsumerEventHandler<ActivityDto>
    ): ConsumerWorker<ActivityDto> {
        val consumer: RaribleKafkaConsumer<ActivityDto> = consumer(ACTIVITY)
        return ConsumerWorker(
            consumer = consumer,
            properties = kafkaProperties.daemon,
            eventHandler = handler,
            meterRegistry = meterRegistry,
            workerName = worker(ACTIVITY),
        )
    }

    private inline fun <reified T> consumer(suffix: String): RaribleKafkaConsumer<T> {
        return RaribleKafkaConsumer(
            clientId = clientId(suffix),
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = T::class.java,
            consumerGroup = consumerGroup(suffix),
            defaultTopic = topic(suffix),
            bootstrapServers = kafkaProperties.brokerReplicaSet,
        )
    }

    private fun clientId(suffix: String): String {
        return "$env.$host.${UUID.randomUUID()}.$suffix-consumer"
    }

    private fun consumerGroup(suffix: String): String {
        return "$env.protocol.union.search.$suffix"
    }

    private fun topic(suffix: String): String {
        return "$env.protocol-union-service.$suffix"
    }

    private fun worker(suffix: String): String {
        return "$env.$suffix.worker"
    }
}
