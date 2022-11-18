package com.rarible.protocol.union.core

open class DefaultConsumerProperties(
    val brokerReplicaSet: String?,
    val workers: Map<String, Int>,
    val batchSize: Int = 32, // TODO ideally, make it configurable per topic
    val username: String? = null,
    val password: String? = null
)
