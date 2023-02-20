package com.rarible.protocol.union.worker.config

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "worker")
data class WorkerProperties(
    val searchReindex: SearchReindexProperties,
    val metrics: MetricsProperties = MetricsProperties(),
    val reconciliation: ReconciliationProperties,
    val collectionStatisticsResync: CollectionStatisticsResyncProperties,
    val platformBestSellCleanup: PlatformBestSellCleanUpProperties = PlatformBestSellCleanUpProperties(),
    val priceUpdate: PriceUpdateProperties,
    val reconcileMarks: ReconcileMarksProperties,
    val metaItemRetry: MetaItemRetry,
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

class CollectionStatisticsResyncProperties(
    val enabled: Boolean = false,
    val rate: Duration = Duration.ofHours(12),
    val limit: Int = 50
)

data class PlatformBestSellCleanUpProperties(
    val enabled: Boolean = true,
    val itemBatchSize: Int = 100,
    val ownershipBatchSize: Int = 100
)

class PriceUpdateProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(5)
)

class ReconcileMarksProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofSeconds(15)
)

class MetaItemRetry(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(1)
)
