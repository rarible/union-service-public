package com.rarible.protocol.union.worker.config

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "worker")
data class WorkerProperties(
    val searchReindex: SearchReindexProperties = SearchReindexProperties(),
    val metrics: MetricsProperties = MetricsProperties(),
    val reconciliation: ReconciliationProperties = ReconciliationProperties(),
    val platformBestSellCleanup: PlatformBestSellCleanUpProperties = PlatformBestSellCleanUpProperties(),
    val priceUpdate: PriceUpdateProperties = PriceUpdateProperties(),
    val metaRefreshRequestCleanup: MetaRefreshRequestCleanupProperties = MetaRefreshRequestCleanupProperties(),
    val metaAutoRefresh: MetaAutoRefreshProperties = MetaAutoRefreshProperties(),
    val metaRefresh: MetaRefreshProperties = MetaRefreshProperties(),
    val reconcileMarks: ReconcileMarksProperties = ReconcileMarksProperties(),
    val metaItemRetry: MetaRetry = MetaRetry(),
    val metaCollectionRetry: MetaRetry = MetaRetry(),
    val itemMetaCustomAttributesJob: ItemMetaCustomAttributesJobProperties = ItemMetaCustomAttributesJobProperties(),
    val communityMarketplace: CommunityMarketplaceProperties = CommunityMarketplaceProperties(),
)

data class SearchReindexProperties(
    val activity: ActivityReindexProperties = ActivityReindexProperties(),
    val order: OrderReindexProperties = OrderReindexProperties(),
    val collection: CollectionReindexProperties = CollectionReindexProperties(),
    val ownership: OwnershipReindexProperties = OwnershipReindexProperties(),
    val item: ItemReindexProperties = ItemReindexProperties(),
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
    override val enabled: Boolean = true,
    override val blockchains: List<BlockchainReindexProperties> = emptyList()
) : EntityReindexProperties()

data class OrderReindexProperties(
    override val enabled: Boolean = true,
    override val blockchains: List<BlockchainReindexProperties> = emptyList()
) : EntityReindexProperties()

class CollectionReindexProperties(
    override val enabled: Boolean = true,
    override val blockchains: List<BlockchainReindexProperties> = emptyList()
) : EntityReindexProperties()

class ItemReindexProperties(
    override val enabled: Boolean = true,
    override val blockchains: List<BlockchainReindexProperties> = emptyList()
) : EntityReindexProperties()

data class OwnershipReindexProperties(
    override val enabled: Boolean = true,
    override val blockchains: List<BlockchainReindexProperties> = emptyList()
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

data class PlatformBestSellCleanUpProperties(
    val enabled: Boolean = true,
    val itemBatchSize: Int = 100,
    val ownershipBatchSize: Int = 100
)

class PriceUpdateProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(5)
)

class MetaRefreshRequestCleanupProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(5)
)

class MetaAutoRefreshProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofHours(2),
    val errorDelay: Duration = Duration.ofMinutes(1),
    val createdPeriod: Duration = Duration.ofDays(30),
    val refreshedPeriod: Duration = Duration.ofDays(14),
    val numberOfCollectionsToCheck: Int = 100
)

class MetaRefreshProperties(
    val enabled: Boolean = true,
    val maxTaskQueueSize: Long = 1000,
    val concurrency: Int = 10,
    val rate: Duration = Duration.ofSeconds(30),
)

class ReconcileMarksProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofSeconds(15)
)

class MetaRetry(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofMinutes(1)
)

class ItemMetaCustomAttributesJobProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofDays(1),
    val providers: ItemMetaCustomAttributesProviderProperties = ItemMetaCustomAttributesProviderProperties()
)

class ItemMetaCustomAttributesProviderProperties(
    val mocaXp: MocaXpCustomAttributesProviderProperties = MocaXpCustomAttributesProviderProperties()
)

class MocaXpCustomAttributesProviderProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val collection: String = "",
    val uri: String = "",
    val apiKey: String = "",
)

data class CommunityMarketplaceProperties(
    val communityMarketplaceUrl: String = "http://127.0.0.1:8080",
)
