package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("enrichment_collection")
data class ShortCollection(
    val blockchain: BlockchainDto,
    val collectionId: String,

    val bestSellOrders: Map<String, ShortOrder>,
    val bestBidOrders: Map<String, ShortOrder>,

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val bestSellOrder: ShortOrder?,
    val bestBidOrder: ShortOrder?,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) {
    fun withCalculatedFields(): ShortCollection {
        return this.copy(
            multiCurrency = bestSellOrders.size > 1 || bestBidOrders.size > 1,
            lastUpdatedAt = nowMillis()
        )
    }

    companion object {
        fun empty(collectionId: ShortCollectionId): ShortCollection {
            return ShortCollection(
                version = null,
                blockchain = collectionId.blockchain,
                collectionId = collectionId.collectionId,

                bestSellOrders = emptyMap(),
                bestBidOrders = emptyMap(),

                bestSellOrder = null,
                bestBidOrder = null,

                lastUpdatedAt = nowMillis()
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestBidOrder != null || bestSellOrder != null
    }

    @Transient
    private val _id: ShortCollectionId = ShortCollectionId(blockchain, collectionId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortCollectionId
        get() = _id
        set(_) {}

}