package com.rarible.protocol.union.worker.config

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "worker")
data class WorkerProperties(
    val searchReindex: SearchReindexProperties = SearchReindexProperties(),
    val reconciliation: ReconciliationProperties = ReconciliationProperties(),
    val platformBestSellCleanup: PlatformBestSellCleanUpProperties = PlatformBestSellCleanUpProperties(),
    val priceUpdate: PriceUpdateProperties = PriceUpdateProperties(),
    val metaRefreshRequestCleanup: MetaRefreshRequestCleanupProperties = MetaRefreshRequestCleanupProperties(),
    val traitsWithZeroItemsCountCleanUp: TraitsWithZeroItemsCountCleanUpProperties = TraitsWithZeroItemsCountCleanUpProperties(),
    val metaAutoRefresh: MetaAutoRefreshProperties = MetaAutoRefreshProperties(),
    val metaRefresh: MetaRefreshProperties = MetaRefreshProperties(),
    val reconcileMarks: ReconcileMarksProperties = ReconcileMarksProperties(),
    val metaItemRetry: MetaRetry = MetaRetry(),
    val metaCollectionRetry: MetaRetry = MetaRetry(),
    val itemMetaCustomAttributesJob: ItemMetaCustomAttributesJobProperties = ItemMetaCustomAttributesJobProperties(),
    val communityMarketplace: CommunityMarketplaceProperties = CommunityMarketplaceProperties(),
    val ratelimiter: RateLimiterProperties = RateLimiterProperties()
)

data class SearchReindexProperties(
    val activity: ActivityReindexProperties = ActivityReindexProperties(),
    val order: OrderReindexProperties = OrderReindexProperties(),
    val collection: CollectionReindexProperties = CollectionReindexProperties(),
    val ownership: OwnershipReindexProperties = OwnershipReindexProperties(),
    val item: ItemReindexProperties = ItemReindexProperties(),
    val trait: TraitReindexProperties = TraitReindexProperties(),
)

sealed class EntityReindexProperties {
    abstract val enabled: Boolean
}

data class ActivityReindexProperties(
    override val enabled: Boolean = true,
) : EntityReindexProperties()

class CollectionReindexProperties(
    override val enabled: Boolean = true,
) : EntityReindexProperties()

class ItemReindexProperties(
    override val enabled: Boolean = true,
) : EntityReindexProperties()

data class OwnershipReindexProperties(
    override val enabled: Boolean = true,
) : EntityReindexProperties()

data class OrderReindexProperties(
    override val enabled: Boolean = false,
) : EntityReindexProperties()

data class BlockchainReindexProperties(
    val enabled: Boolean,
    val blockchain: BlockchainDto
)

class TraitReindexProperties(
    override val enabled: Boolean = true,
) : EntityReindexProperties()

class ReconciliationProperties(
    val collectionBatchSize: Int = 50,
    val orderBatchSize: Int = 100,
    val auctionBatchSize: Int = 50,
    val activityBatchSize: Int = 100,
    val threadCount: Int = 8,
    val notificationEnabled: Boolean = true
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

class TraitsWithZeroItemsCountCleanUpProperties(
    val enabled: Boolean = true,
    val rate: Duration = Duration.ofHours(24)
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
    val rate: Duration = Duration.ofSeconds(1)
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

data class RateLimiterProperties(
    val period: Long = 10000,
    val maxEntities: Int = 10000,
)
