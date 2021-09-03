package com.rarible.protocol.union.listener.config.activity

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("protocol.flow-nft.subscriber")
@ConstructorBinding
data class FlowActivityEventsSubscriberProperties(
    val brokerReplicaSet: String
)
