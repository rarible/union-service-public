package com.rarible.protocol.union.listener.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("listener.event-producer")
data class UnionEventProducerProperties(
    val environment: String,
    val kafkaReplicaSet: String
)
