package com.rarible.protocol.union.subscriber.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("protocol.union.subscriber")
@ConstructorBinding
data class UnionEventsSubscriberProperties(
    val brokerReplicaSet: String
)