package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("listener")
class UnionListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties()
)