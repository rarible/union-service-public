package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.time.Instant

@ConstructorBinding
@ConfigurationProperties("listener")
data class UnionListenerProperties(
    val consumer: InternalConsumerProperties,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val reconciliation: ReconciliationProperties,
    val openSeaCleanup: OpenSeaCleanUpProperties,
    val priceUpdate: PriceUpdateProperties,
    val reconcileMarks: ReconcileMarksProperties,
    val metaScheduling: MetaSchedulingProperties,
    val metrics: MetricsProperties
)

class InternalConsumerProperties(
    val brokerReplicaSet: String,
    val workers: Map<String, Int>,
    val blockchainWorkers: Map<String, Int> = emptyMap()
)

class ReconciliationProperties(
    val collectionBatchSize: Int = 50,
    val orderBatchSize: Int = 50,
    val auctionBatchSize: Int = 50,
    val activityBatchSize: Int = 100,
    val threadCount: Int = 4,
    val notificationEnabled: Boolean = true
)

class ReconcileMarksProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofSeconds(15)
)

class PriceUpdateProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(5)
)

data class OpenSeaCleanUpProperties(
    val enabled: Boolean = true,
    val sellOrderFrom: Instant?,
    val itemBatchSize: Int = 100,
    val ownershipBatchSize: Int = 100
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
