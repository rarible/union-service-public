package com.rarible.protocol.union.api.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("api.subscribe")
data class SubscribeProperties(
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    val workers: Map<String, Int>
)