package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(ShortOwnership.COLLECTION)
data class ShortOwnership(

    val blockchain: BlockchainDto,
    val itemId: String, // ItemId without blockchain prefix
    val owner: String, // Owner without blockchain prefix

    override val bestSellOrder: ShortOrder? = null,
    override val bestSellOrders: Map<String, ShortOrder>,

    override val originOrders: Set<OriginOrders> = emptySet(),

    val multiCurrency: Boolean = bestSellOrders.size > 1,

    val source: OwnershipSourceDto? = null,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) : BestSellOrderOwner<ShortOwnership>, OriginOrdersOwner {

    fun withCalculatedFields(): ShortOwnership {
        return copy(
            multiCurrency = bestSellOrders.size > 1,
            lastUpdatedAt = nowMillis()
        )
    }

    companion object {
        const val COLLECTION = "enrichment_ownership"

        fun empty(ownershipId: ShortOwnershipId): ShortOwnership {
            return ShortOwnership(
                blockchain = ownershipId.blockchain,
                itemId = ownershipId.itemId,
                owner = ownershipId.owner,

                bestSellOrders = emptyMap(),
                bestSellOrder = null,
                originOrders = emptySet(),
                lastUpdatedAt = nowMillis(),

                version = null,
                source = null
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestSellOrder != null || source != null
    }

    @Transient
    private val _id: ShortOwnershipId = ShortOwnershipId(blockchain, itemId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortOwnershipId
        get() = _id
        set(_) {}

    override fun withBestSellOrders(orders: Map<String, ShortOrder>): ShortOwnership {
        return this.copy(bestSellOrders = orders)
    }

    override fun withBestSellOrder(order: ShortOrder?): ShortOwnership {
        return this.copy(bestSellOrder = order)
    }

    override fun getAllBestOrders(): List<ShortOrder> {
        return listOfNotNull(bestSellOrder) + getAllOriginBestOrders()
    }
}
