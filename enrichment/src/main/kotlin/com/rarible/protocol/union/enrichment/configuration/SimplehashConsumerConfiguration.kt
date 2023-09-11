package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.core.kafka.RaribleKafkaMessageListenerFactory
import com.rarible.simplehash.client.subcriber.SimplehashKafkaAvroDeserializer
import com.simplehash.v0.nft
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.AbstractMessageListenerContainer
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.util.UUID

@ConditionalOnProperty("meta.simplehash.kafka.enabled", havingValue = "true")
class SimplehashConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name
    private val clientId = "$env.${UUID.randomUUID()}"

    private val logger = LoggerFactory.getLogger(javaClass)

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
        val settings = if (!kafkaProps.username.isNullOrEmpty()) {
            logger.info("Connecting to ${kafkaProps.broker} using username=${kafkaProps.username} and password=****${kafkaProps.password?.takeLast(5)}")
            avroConfig + mapOf(
                "security.protocol" to "SASL_SSL",
                "sasl.mechanism" to "PLAIN",
                "sasl.jaas.config" to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"${kafkaProps.username}\" password=\"${kafkaProps.password}\";"
            )
        } else {
            logger.info("Connecting to ${kafkaProps.broker} without credentials")
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
        logger.info("Creating consumers for topics: ${props.simpleHash.kafka.topics}")
        val containers = props.simpleHash.kafka.topics.map {
            val container = factory.createContainer(it)
            container.setupMessageListener(listener)
            container.containerProperties.groupId = "rarible-$env"
            container.containerProperties.clientId = "rarible-$clientId"
            container
        }

        return SimplehashConsumerWorkerWrapper(containers)
    }

    private class SimplehashConsumerWorkerWrapper<K, V>(
        private val containers: List<AbstractMessageListenerContainer<K, V>>
    ) : RaribleKafkaConsumerWorker<V>, ApplicationEventPublisherAware, ApplicationContextAware {

        override fun start() {
            containers.forEach(AbstractMessageListenerContainer<K, V>::start)
        }

        override fun close() {
            containers.forEach(AbstractMessageListenerContainer<K, V>::start)
        }

        override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
            containers.forEach { it.setApplicationEventPublisher(applicationEventPublisher) }
        }

        override fun setApplicationContext(applicationContext: ApplicationContext) {
            containers.forEach { it.setApplicationContext(applicationContext) }
        }
    }
}
