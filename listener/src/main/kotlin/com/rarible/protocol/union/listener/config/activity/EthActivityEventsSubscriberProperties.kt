package com.rarible.protocol.union.listener.config.activity

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val PROTOCOL_ACTIVITY_SUBSCRIBER = "protocol.activity.subscriber"

@ConfigurationProperties(PROTOCOL_ACTIVITY_SUBSCRIBER)
@ConstructorBinding
data class EthActivityEventsSubscriberProperties(
    val brokerReplicaSet: String
)
