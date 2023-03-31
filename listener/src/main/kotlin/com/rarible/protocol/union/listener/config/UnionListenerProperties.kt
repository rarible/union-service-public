package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("listener")
data class UnionListenerProperties(
    val consumer: InternalConsumerProperties,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val metaScheduling: MetaSchedulingProperties,
    val metrics: MetricsProperties
)

class InternalConsumerProperties(
    val brokerReplicaSet: String,
    val workers: Map<String, Int>,
    val blockchainWorkers: Map<String, Int> = emptyMap()
)

data class MetricsProperties(
    val rootPath: String = "protocol.union.listener"
)

data class MetaSchedulingProperties(
    val item: MetaEntrySchedulingProperties = MetaEntrySchedulingProperties(),
    val collection: MetaEntrySchedulingProperties = MetaEntrySchedulingProperties()
)

data class MetaEntrySchedulingProperties(
    val workers: Int = 3,
    val batchSize: Int = 500
)
