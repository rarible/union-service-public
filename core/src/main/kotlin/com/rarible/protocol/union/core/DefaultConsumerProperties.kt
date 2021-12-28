package com.rarible.protocol.union.core

open class DefaultConsumerProperties(
    val brokerReplicaSet: String?,
    val workers: Map<String, Int>,
    val username: String? = null,
    val password: String? = null
)
