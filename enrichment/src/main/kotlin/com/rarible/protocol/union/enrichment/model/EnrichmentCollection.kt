package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Table with additional info about collection, e.g. best sell/bid orders.
 */
@Document("enrichment_collection")
data class EnrichmentCollection(
    val blockchain: BlockchainDto,
    val collectionId: String,

    val name: String,
    val status: UnionCollection.Status? = null,
    val type: UnionCollection.Type? = null,
    val minters: List<UnionAddress>? = listOf(),
    val features: List<UnionCollection.Features> = listOf(),
    val owner: UnionAddress? = null,
    val parent: EnrichmentCollectionId? = null,
    val symbol: String? = null,
    val self: Boolean? = null,

    override val bestSellOrder: ShortOrder? = null,
    override val bestSellOrders: Map<String, ShortOrder>,

    override val bestBidOrder: ShortOrder? = null,
    override val bestBidOrders: Map<String, ShortOrder>,

    override val originOrders: Set<OriginOrders> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val lastUpdatedAt: Instant,

    val metaEntry: DownloadEntry<UnionCollectionMeta>? = null,

    @Version
    val version: Long? = null
) : BestSellOrderOwner<EnrichmentCollection>, BestBidOrderOwner<EnrichmentCollection>, OriginOrdersOwner {

    fun withCalculatedFieldsAndUpdatedAt(): EnrichmentCollection {
        return this.copy(
            multiCurrency = bestSellOrders.size > 1 || bestBidOrders.size > 1,
            lastUpdatedAt = nowMillis()
        )
    }

    @Deprecated("Required for migration only, remove later")
    fun withCalculatedFields(): EnrichmentCollection {
        return this.copy(
            multiCurrency = bestSellOrders.size > 1 || bestBidOrders.size > 1
        )
    }

    fun isNotEmpty(): Boolean {
        return bestBidOrder != null || bestSellOrder != null
    }

    @Transient
    private val _id: EnrichmentCollectionId = EnrichmentCollectionId(blockchain, collectionId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: EnrichmentCollectionId
        get() = _id
        set(_) {}

    fun withMeta(entry: DownloadEntry<UnionCollectionMeta>): EnrichmentCollection {
        val metaChanged = this.metaEntry?.data != entry.data
        return this.copy(
            metaEntry = entry,
            lastUpdatedAt = if (metaChanged) nowMillis() else lastUpdatedAt
        )
    }

    override fun withBestBidOrders(orders: Map<String, ShortOrder>): EnrichmentCollection {
        return this.copy(bestBidOrders = orders)
    }

    override fun withBestBidOrder(order: ShortOrder?): EnrichmentCollection {
        return this.copy(bestBidOrder = order)
    }

    override fun withBestSellOrders(orders: Map<String, ShortOrder>): EnrichmentCollection {
        return this.copy(bestSellOrders = orders)
    }

    override fun withBestSellOrder(order: ShortOrder?): EnrichmentCollection {
        return this.copy(bestSellOrder = order)
    }

    fun withNextRetry(): EnrichmentCollection {
        return this.copy(
            metaEntry = metaEntry?.copy(retries = metaEntry.retries + 1, retriedAt = nowMillis())
        )
    }

    fun withData(collection: UnionCollection): EnrichmentCollection {
        val converted = EnrichmentCollectionConverter.convert(collection)
        return this.copy(
            name = converted.name,
            status = converted.status,
            type = converted.type,
            minters = converted.minters,
            features = converted.features,
            owner = converted.owner,
            parent = converted.parent,
            symbol = converted.symbol,
            self = converted.self
        )
    }

    override fun getAllBestOrders(): List<ShortOrder> {
        return listOfNotNull(bestSellOrder, bestBidOrder) + getAllOriginBestOrders()
    }
}
