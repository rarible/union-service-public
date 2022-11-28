package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("listener")
data class UnionListenerProperties(
    val consumer: InternalConsumerProperties,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val priceUpdate: PriceUpdateProperties,
    val metaItemRetry: MetaItemRetry,
    val reconcileMarks: ReconcileMarksProperties,
    val metaScheduling: MetaSchedulingProperties,
    val metrics: MetricsProperties
)

class InternalConsumerProperties(
    val brokerReplicaSet: String,
    val workers: Map<String, Int>,
    val blockchainWorkers: Map<String, Int> = emptyMap()
)

class ReconcileMarksProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofSeconds(15)
)

class MetaItemRetry(
    val enabled: Boolean = false,
    val rate: Duration = Duration.ofMinutes(1)
)

class PriceUpdateProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(5)
)

data class MetricsProperties(
    val rootPath: String = "protocol.union.listener"
)

data class MetaSchedulingProperties(
    val item: MetaEntrySchedulingProperties = MetaEntrySchedulingProperties(),
    // TODO add collection
)

data class MetaEntrySchedulingProperties(
    val workers: Int = 4,
    val batchSize: Int = 16
)
