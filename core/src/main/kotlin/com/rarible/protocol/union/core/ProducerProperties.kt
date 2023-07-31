package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common.producer")
class ProducerProperties(
    val brokerReplicaSet: String
)
