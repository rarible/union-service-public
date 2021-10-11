package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document("item")
data class ShortItem(

    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,

    val sellers: Int = 0,
    val totalStock: BigInteger,

    val bestSellOrders: Map<CurrencyId, ShortOrder>,
    val bestBidOrders: Map<CurrencyId, ShortOrder>,

    val bestSellOrderCount: Int = 0,
    val bestBidOrderCount: Int = 0,

    val bestSellOrder: ShortOrder?,
    val bestBidOrder: ShortOrder?,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) {

    fun withBestSellOrders(
        bestSellOrder: ShortOrder?,
        bestSellOrders: Map<CurrencyId, ShortOrder>
    ): ShortItem {
        return copy(
            bestSellOrder = bestSellOrder,
            bestSellOrders = bestSellOrders,
            bestSellOrderCount = bestSellOrders.size,
            lastUpdatedAt = nowMillis()
        )
    }

    fun withBestBidOrders(
        bestBidOrder: ShortOrder?,
        bestBidOrders: Map<CurrencyId, ShortOrder>
    ): ShortItem {
        return copy(
            bestBidOrder = bestBidOrder,
            bestBidOrders = bestBidOrders,
            bestBidOrderCount = bestBidOrders.size,
            lastUpdatedAt = nowMillis()
        )
    }

    companion object {
        fun empty(itemId: ShortItemId): ShortItem {
            return ShortItem(
                version = null,
                blockchain = itemId.blockchain,
                token = itemId.token,
                tokenId = itemId.tokenId,

                sellers = 0,
                totalStock = BigInteger.ZERO,

                bestSellOrders = emptyMap(),
                bestBidOrders = emptyMap(),

                bestSellOrderCount = 0,
                bestBidOrderCount = 0,

                bestSellOrder = null,
                bestBidOrder = null,

                lastUpdatedAt = nowMillis()
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestBidOrder != null || bestSellOrder != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortItem

        if (blockchain != other.blockchain) return false
        if (token != other.token) return false
        if (tokenId != other.tokenId) return false
        if (sellers != other.sellers) return false
        if (totalStock != other.totalStock) return false
        if (bestSellOrder?.clearState() != other.bestSellOrder?.clearState()) return false
        if (bestBidOrder?.clearState() != other.bestBidOrder?.clearState()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockchain.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + tokenId.hashCode()
        result = 31 * result + sellers
        result = 31 * result + totalStock.hashCode()
        result = 31 * result + (bestSellOrder?.hashCode() ?: 0)
        result = 31 * result + (bestBidOrder?.hashCode() ?: 0)
        return result
    }

    @Transient
    private val _id: ShortItemId = ShortItemId(blockchain, token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortItemId
        get() = _id
        set(_) {}

}



