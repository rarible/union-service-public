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

    @Transient
    private val _id: ShortOwnershipId = ShortOwnershipId(blockchain, token, tokenId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortOwnershipId
        get() = _id
        set(_) {}

}
