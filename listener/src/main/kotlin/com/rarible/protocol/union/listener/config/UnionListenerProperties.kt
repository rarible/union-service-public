package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.kafka.Compression
import com.rarible.protocol.union.dto.BlockchainDto
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
    val compression: Compression = Compression.SNAPPY,
    private val workers: Map<String, BlockchainWorkerProperties> = emptyMap()
) {

    fun getWorkerProperties(blockchain: BlockchainDto): BlockchainWorkerProperties {
        return workers[blockchain.name.lowercase()] ?: BlockchainWorkerProperties()
    }
}

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

data class BlockchainWorkerProperties(
    val concurrency: Int = 9,
    val batchSize: Int = 500
)
