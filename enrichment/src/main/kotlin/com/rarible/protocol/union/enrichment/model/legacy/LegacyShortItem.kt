package com.rarible.protocol.union.enrichment.model.legacy

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOrder
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document("item")
@Deprecated("Should be replaced by implementation without token/tokenId")
data class LegacyShortItem(

    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,

    val sellers: Int = 0,
    val totalStock: BigInteger,

    val bestSellOrders: Map<String, ShortOrder>,
    val bestBidOrders: Map<String, ShortOrder>,

    val auctions: Set<AuctionIdDto> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val bestSellOrder: ShortOrder?,
    val bestBidOrder: ShortOrder?,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) {

    constructor(shortItem: ShortItem) :
        this(
            version = shortItem.version,
            blockchain = shortItem.blockchain,
            token = shortItem.itemId.substringBefore(IdParser.DELIMITER),
            tokenId = shortItem.itemId.substringAfter(IdParser.DELIMITER).toBigInteger(),

            sellers = shortItem.sellers,
            totalStock = shortItem.totalStock,

            bestSellOrders = shortItem.bestSellOrders,
            bestBidOrders = shortItem.bestBidOrders,

            auctions = shortItem.auctions,

            multiCurrency = shortItem.multiCurrency,

            bestSellOrder = shortItem.bestSellOrder,
            bestBidOrder = shortItem.bestBidOrder,

            lastUpdatedAt = shortItem.lastUpdatedAt
        )

    fun toShortItem(): ShortItem {
        return ShortItem(
            version = version,
            blockchain = blockchain,
            itemId = "$token:$tokenId",

            sellers = sellers,
            totalStock = totalStock,

            bestSellOrders = bestSellOrders,
            bestBidOrders = bestBidOrders,

            auctions = auctions,
            multiCurrency = multiCurrency,

            bestSellOrder = bestSellOrder,
            bestBidOrder = bestBidOrder,

            lastUpdatedAt = lastUpdatedAt
        )
    }

    companion object {

        fun empty(itemId: LegacyShortItemId): LegacyShortItem {
            return LegacyShortItem(
                version = null,
                blockchain = itemId.blockchain,
                token = itemId.token,
                tokenId = itemId.tokenId,

                sellers = 0,
                totalStock = BigInteger.ZERO,

                bestSellOrders = emptyMap(),
                bestBidOrders = emptyMap(),

                auctions = emptySet(),

                bestSellOrder = null,
                bestBidOrder = null,

                lastUpdatedAt = nowMillis()
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestBidOrder != null || bestSellOrder != null || auctions.isNotEmpty()
    }

    @Transient
    private val _id: LegacyShortItemId = LegacyShortItemId(blockchain, token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: LegacyShortItemId
        get() = _id
        set(_) {}

}



