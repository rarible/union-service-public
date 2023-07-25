package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.apikey.kafka.RaribleKafkaMessageListenerFactory
import com.rarible.simplehash.client.subcriber.SimplehashKafkaAvroDeserializer
import com.simplehash.v0.nft
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.util.UUID


@ConditionalOnProperty("meta.simpleHash.kafka.enabled", havingValue = "true")
class SimplehashConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name
    private val clientId = "$env.${UUID.randomUUID()}"

    @Bean
    fun simplehashConsumerFactory(props: UnionMetaProperties): RaribleKafkaListenerContainerFactory<nft> {
        val kafkaProps = props.simpleHash.kafka
        val avroConfig = mapOf(

            // To simplify integration we don't use registry
            // In case of appearing new schema we need to generate new model anyway
            "auto.register.schemas" to false,
            "schema.registry.url" to "http://localhost",

            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to SimplehashKafkaAvroDeserializer::class.java
        )
        val settings = if (kafkaProps.username != null) {
            avroConfig + mapOf(
                "security.protocol" to "SASL_SSL",
                "sasl.mechanism" to "PLAIN",
                "sasl.jaas.config" to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"${kafkaProps.username}\" password=\"${kafkaProps.password}\";"
            )
        } else {
            avroConfig
        }
        return RaribleKafkaListenerContainerFactory(
            hosts = kafkaProps.broker,
            concurrency = kafkaProps.concurrency,
            batchSize = kafkaProps.batchSize,
            offsetResetStrategy = OffsetResetStrategy.LATEST,
            valueClass = nft::class.java,
            customSettings = settings
        )
    }

    @Bean
    fun simplehashWorker(
        props: UnionMetaProperties,
        factory: RaribleKafkaListenerContainerFactory<nft>,
        handler: RaribleKafkaBatchEventHandler<nft>
    ): RaribleKafkaConsumerWorker<nft> {
        val listener = RaribleKafkaMessageListenerFactory.create(handler, true)
        val containers = props.simpleHash.kafka.topics.map {
            val container = factory.createContainer(it)
            container.setupMessageListener(listener)
            container.containerProperties.groupId = "rarible-${env}"
            container.containerProperties.clientId = "rarible-${clientId}"
            container
        }

        return RaribleKafkaConsumerFactory.RaribleKafkaConsumerWorkerWrapper(containers)
    }

}
