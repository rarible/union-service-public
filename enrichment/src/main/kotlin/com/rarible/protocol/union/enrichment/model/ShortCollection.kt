package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
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
 * If there is no such info, related collection doesn't present in this table.
 */
@Document("enrichment_collection")
data class ShortCollection(
    val blockchain: BlockchainDto,
    val collectionId: String,

    override val bestSellOrder: ShortOrder? = null,
    override val bestSellOrders: Map<String, ShortOrder>,

    override val bestBidOrder: ShortOrder? = null,
    override val bestBidOrders: Map<String, ShortOrder>,

    override val originOrders: Set<OriginOrders> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1 || bestBidOrders.size > 1,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) : BestSellOrderOwner<ShortCollection>, BestBidOrderOwner<ShortCollection>, OriginOrdersOwner {

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

                bestSellOrder = null,
                bestSellOrders = emptyMap(),

                originOrders = emptySet(),

                bestBidOrder = null,
                bestBidOrders = emptyMap(),

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

    override fun withBestBidOrders(orders: Map<String, ShortOrder>): ShortCollection {
        return this.copy(bestBidOrders = orders)
    }

    override fun withBestBidOrder(order: ShortOrder?): ShortCollection {
        return this.copy(bestBidOrder = order)
    }

    override fun withBestSellOrders(orders: Map<String, ShortOrder>): ShortCollection {
        return this.copy(bestSellOrders = orders)
    }

    override fun withBestSellOrder(order: ShortOrder?): ShortCollection {
        return this.copy(bestSellOrder = order)
    }

    override fun getAllBestOrders(): List<ShortOrder> {
        return listOfNotNull(bestSellOrder, bestBidOrder) + getAllOriginBestOrders()
    }
}
