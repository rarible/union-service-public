package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
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

    val bestSellOrders: Map<String, ShortOrder>,
    val bestBidOrders: Map<String, ShortOrder>,

    val auctions: Set<AuctionIdDto> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val bestSellOrder: ShortOrder?,
    val bestBidOrder: ShortOrder?,

    val lastSale: ItemLastSale?,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) {

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

                bestSellOrders = emptyMap(),
                bestBidOrders = emptyMap(),

                auctions = emptySet(),

                bestSellOrder = null,
                bestBidOrder = null,

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

}



