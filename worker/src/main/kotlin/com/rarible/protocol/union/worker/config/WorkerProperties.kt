package com.rarible.protocol.union.worker.config

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "worker")
data class WorkerProperties(
    val searchReindex: SearchReindexProperties,
    val metrics: MetricsProperties = MetricsProperties(),
    val reconciliation: ReconciliationProperties,
)

data class SearchReindexProperties(
    val activity: ActivityReindexProperties,
    val order: OrderReindexProperties,
    val collection: CollectionReindexProperties,
    val ownership: OwnershipReindexProperties,
    val item: ItemReindexProperties,
)

sealed class EntityReindexProperties {
    abstract val enabled: Boolean
    abstract val blockchains: List<BlockchainReindexProperties>

    fun activeBlockchains(): List<BlockchainDto> {
        if (!this.enabled) return emptyList()

        return blockchains.filter { it.enabled }.map { it.blockchain }
    }

    fun isBlockchainActive(blockchain: BlockchainDto): Boolean {
        return this.enabled && this
            .blockchains
            .singleOrNull { it.blockchain == blockchain }
            ?.enabled ?: false
    }
}

data class ActivityReindexProperties(
    override val enabled: Boolean,
    override val blockchains: List<BlockchainReindexProperties>
) : EntityReindexProperties()

data class OrderReindexProperties(
    override val enabled: Boolean,
    override val blockchains: List<BlockchainReindexProperties>
) : EntityReindexProperties()

class CollectionReindexProperties(
    override val enabled: Boolean,
    override val blockchains: List<BlockchainReindexProperties>
) : EntityReindexProperties()

class ItemReindexProperties(
    override val enabled: Boolean,
    override val blockchains: List<BlockchainReindexProperties>
) : EntityReindexProperties()

data class OwnershipReindexProperties(
    override val enabled: Boolean,
    override val blockchains: List<BlockchainReindexProperties>
) : EntityReindexProperties()

data class BlockchainReindexProperties(
    val enabled: Boolean,
    val blockchain: BlockchainDto
)

class ReconciliationProperties(
    val collectionBatchSize: Int = 50,
    val orderBatchSize: Int = 50,
    val auctionBatchSize: Int = 50,
    val activityBatchSize: Int = 100,
    val threadCount: Int = 4,
    val notificationEnabled: Boolean = true
)

data class MetricsProperties(
    val rootPath: String = "protocol.union.worker"
)
