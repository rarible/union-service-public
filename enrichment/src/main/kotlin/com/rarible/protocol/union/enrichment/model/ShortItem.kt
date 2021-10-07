package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger

@Document("item")
data class ShortItem(

    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,

    val sellers: Int = 0,
    val totalStock: BigInteger,

    val bestSellOrders: Map<CurrencyId, ShortOrder>,
    val bestBidOrders: Map<CurrencyId, ShortOrder>,

    val bestSellOrder: ShortOrder?,
    val bestBidOrder: ShortOrder?,

    @Version
    val version: Long? = null
) {

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

                bestSellOrder = null,
                bestBidOrder = null
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestBidOrder != null || bestSellOrder != null
    }

    @Transient
    private val _id: ShortItemId = ShortItemId(blockchain, token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortItemId
        get() = _id
        set(_) {}

}



