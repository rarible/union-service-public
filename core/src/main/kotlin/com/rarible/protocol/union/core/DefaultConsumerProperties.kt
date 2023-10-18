package com.rarible.protocol.union.core

open class DefaultConsumerProperties(
    val brokerReplicaSet: String?,
    val workers: Map<String, Int> = mapOf(),
    val batchSize: Int = 500, // TODO ideally, make it configurable per topic
    val username: String? = null,
    val password: String? = null
)
