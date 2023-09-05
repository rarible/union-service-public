package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BlockchainAware
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document(ShortItem.COLLECTION)
data class ShortItem(

    override val blockchain: BlockchainDto,
    val itemId: String,
    val collectionId: String? = null,

    val sellers: Int = 0,
    val totalStock: BigInteger,

    override val bestSellOrder: ShortOrder? = null,
    override val bestSellOrders: Map<String, ShortOrder>,

    override val bestBidOrder: ShortOrder? = null,
    override val bestBidOrders: Map<String, ShortOrder>,

    override val originOrders: Set<OriginOrders> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val auctions: Set<AuctionIdDto> = emptySet(),

    val lastSale: ItemLastSale? = null,

    val lastUpdatedAt: Instant,

    val poolSellOrders: List<ShortPoolOrder> = emptyList(),

    val metaEntry: DownloadEntry<UnionMeta>? = null,

    @Version
    val version: Long? = null
) : BestSellOrderOwner<ShortItem>, BestBidOrderOwner<ShortItem>, OriginOrdersOwner, BlockchainAware {

    @Transient
    private val _id: ShortItemId = ShortItemId(blockchain, itemId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortItemId
        get() = _id
        set(_) {}

    override fun withBestBidOrders(orders: Map<String, ShortOrder>): ShortItem {
        return this.copy(bestBidOrders = orders)
    }

    override fun withBestBidOrder(order: ShortOrder?): ShortItem {
        return this.copy(bestBidOrder = order)
    }

    override fun withBestSellOrders(orders: Map<String, ShortOrder>): ShortItem {
        return this.copy(bestSellOrders = orders)
    }

    override fun withBestSellOrder(order: ShortOrder?): ShortItem {
        return this.copy(bestSellOrder = order)
    }

    override fun getAllBestOrders(): List<ShortOrder> {
        return listOfNotNull(bestSellOrder, bestBidOrder) + getAllOriginBestOrders()
    }

    fun withCalculatedFields(): ShortItem {
        return this.copy(
            multiCurrency = bestSellOrders.size > 1 || bestBidOrders.size > 1,
            lastUpdatedAt = nowMillis()
        )
    }

    fun withNextRetry(): ShortItem {
        return this.copy(
            metaEntry = metaEntry?.copy(retries = metaEntry.retries + 1, retriedAt = nowMillis())
        )
    }

    fun withMeta(entry: DownloadEntry<UnionMeta>): ShortItem {
        val metaChanged = this.metaEntry?.data != entry.data
        return this.copy(
            metaEntry = entry,
            lastUpdatedAt = if (metaChanged) nowMillis() else lastUpdatedAt
        )
    }

    companion object {

        const val COLLECTION = "enrichment_item"

        fun empty(itemId: ShortItemId): ShortItem {
            return ShortItem(
                version = null,
                blockchain = itemId.blockchain,
                itemId = itemId.itemId,

                sellers = 0,
                totalStock = BigInteger.ZERO,

                bestSellOrder = null,
                bestSellOrders = emptyMap(),

                bestBidOrder = null,
                bestBidOrders = emptyMap(),

                originOrders = emptySet(),

                auctions = emptySet(),

                lastSale = null,

                lastUpdatedAt = nowMillis()
            )
        }
    }
}
