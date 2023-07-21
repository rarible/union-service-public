package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.union.integration.ethereum.EthereumConsumerConfiguration
import com.rarible.protocol.union.integration.ethereum.PolygonConsumerConfiguration
import com.rarible.protocol.union.integration.flow.FlowConsumerConfiguration
import com.rarible.protocol.union.integration.immutablex.ImxConsumerConfiguration
import com.rarible.protocol.union.integration.solana.SolanaConsumerConfiguration
import com.rarible.protocol.union.integration.tezos.TezosConsumerConfiguration
import com.simplehash.v0.nft
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.BatchMessageListener
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.util.*


@Configuration
@Import(
    value = [
        EnrichmentApiConfiguration::class,
        EthereumConsumerConfiguration::class,
        PolygonConsumerConfiguration::class,
        FlowConsumerConfiguration::class,
        TezosConsumerConfiguration::class,
        ImxConsumerConfiguration::class,
        SolanaConsumerConfiguration::class
    ]
)
class EnrichmentConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name
    private val clientId = "$env.${UUID.randomUUID()}"

    @Bean
    @ConditionalOnProperty("meta.simpleHash.kafka.enabled", havingValue = "true")
    fun simplehashConsumerFactory(props: UnionMetaProperties): RaribleKafkaListenerContainerFactory<nft> {
        val kafkaProps = props.simpleHash.kafka
        val avroConfig = mapOf(

            // To simplify integration we don't use registry
            // In case of appearing new schema we need to generate new model anyway
            "auto.register.schemas" to false,
            "schema.registry.url" to "http://localhost",

            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to SHKafkaAvroDeserializer::class.java
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
            hosts = props.simpleHash.kafka.broker,
            concurrency = 1,
            batchSize = 1,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = nft::class.java,
            customSettings = settings
        )
    }

    @Bean
    @ConditionalOnProperty("meta.simpleHash.kafka.enabled", havingValue = "true")
    fun simplehashWorker(
        props: UnionMetaProperties,
        factory: RaribleKafkaListenerContainerFactory<nft>,
        listener: BatchMessageListener<String, nft>
    ): RaribleKafkaConsumerWorker<nft> {
        val container = factory.createContainer(props.simpleHash.kafka.topic)
        container.setupMessageListener(listener)
        container.containerProperties.groupId = "rarible-${env}"
        container.containerProperties.clientId = "rarible-${clientId}"

        return RaribleKafkaConsumerFactory.RaribleKafkaConsumerWorkerWrapper(listOf(container))
    }

}
