package com.rarible.protocol.union.core

import com.rarible.core.kafka.Compression
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common.producer")
class ProducerProperties(
    val brokerReplicaSet: String,
    val compression: Compression = Compression.SNAPPY,
)
