package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document("ownership")
data class ShortOwnership(

    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String,

    val bestSellOrders: Map<CurrencyId, ShortOrder>,

    val bestSellOrderCount: Int = 0,

    val bestSellOrder: ShortOrder?,

    val lastUpdatedAt: Instant
) {
    fun withBestSellOrders(
        bestSellOrder: ShortOrder?,
        bestSellOrders: Map<CurrencyId, ShortOrder>
    ): ShortOwnership {
        return copy(
            bestSellOrder = bestSellOrder,
            bestSellOrders = bestSellOrders,
            bestSellOrderCount = bestSellOrders.size,
            lastUpdatedAt = nowMillis()
        )
    }

    companion object {
        fun empty(ownershipId: ShortOwnershipId): ShortOwnership {
            return ShortOwnership(
                blockchain = ownershipId.blockchain,
                token = ownershipId.token,
                tokenId = ownershipId.tokenId,
                owner = ownershipId.owner,

                bestSellOrders = emptyMap(),
                bestSellOrder = null,
                bestSellOrderCount = 0,
                lastUpdatedAt = Instant.EPOCH
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestSellOrder != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortOwnership

        if (blockchain != other.blockchain) return false
        if (token != other.token) return false
        if (tokenId != other.tokenId) return false
        if (owner != other.owner) return false
        if (bestSellOrder?.clearState() != other.bestSellOrder?.clearState()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockchain.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + tokenId.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + (bestSellOrder?.hashCode() ?: 0)
        return result
    }

    @Transient
    private val _id: ShortOwnershipId = ShortOwnershipId(blockchain, token, tokenId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortOwnershipId
        get() = _id
        set(_) {}

}
