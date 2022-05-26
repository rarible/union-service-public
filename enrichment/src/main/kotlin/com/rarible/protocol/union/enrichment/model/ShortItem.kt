package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document("enrichment_item")
data class ShortItem(

    val blockchain: BlockchainDto,
    val itemId: String,

    val sellers: Int = 0,
    val totalStock: BigInteger,

    override val bestSellOrder: ShortOrder?,
    override val bestSellOrders: Map<String, ShortOrder>,

    override val bestBidOrder: ShortOrder?,
    override val bestBidOrders: Map<String, ShortOrder>,

    override val originOrders: Set<OriginOrders> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val auctions: Set<AuctionIdDto> = emptySet(),

    val lastSale: ItemLastSale?,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) : BestSellOrderOwner<ShortItem>, BestBidOrderOwner<ShortItem>, OriginOrdersOwner {

    fun withCalculatedFields(): ShortItem {
        return this.copy(
            multiCurrency = bestSellOrders.size > 1 || bestBidOrders.size > 1,
            lastUpdatedAt = nowMillis()
        )
    }

    companion object {

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

    fun isNotEmpty(): Boolean {
        return bestBidOrder != null || bestSellOrder != null || auctions.isNotEmpty() || lastSale != null
    }

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
}



