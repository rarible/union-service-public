package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("listener")
data class UnionListenerProperties(
    val consumer: InternalConsumerProperties,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val metaScheduling: MetaSchedulingProperties = MetaSchedulingProperties(),
    val metrics: MetricsProperties = MetricsProperties(),
    val communityMarketplace: CommunityMarketplaceProperties = CommunityMarketplaceProperties(),
    val itemChange: ItemChangeProperties = ItemChangeProperties(),
)

class InternalConsumerProperties(
    val brokerReplicaSet: String,
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
    val workers: Int = 9,
    val batchSize: Int = 500
)

data class BlockchainWorkerProperties(
    val concurrency: Int = 9,
    val batchSize: Int = 500,
    val coroutineThreadCount: Int = 1
)

data class CommunityMarketplaceProperties(
    val topic: String = "",
)

data class ItemChangeProperties(
    val workers: Int = 9,
    val batchSize: Int = 500
)
