package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common.event-producer")
data class EnrichmentEventProducerProperties(
    val environment: String,
    val kafkaReplicaSet: String
)
