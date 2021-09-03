package com.rarible.protocol.union.listener.config.activity

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("protocol.activity.subscriber")
@ConstructorBinding
data class EthActivityEventsSubscriberProperties(
    val brokerReplicaSet: String
)
