package com.rarible.protocol.union.enrichment.model.legacy

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document("ownership")
@Deprecated("Should be replaced by implementation without token/tokenId")
data class LegacyShortOwnership(

    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String,

    val bestSellOrders: Map<String, ShortOrder>,

    val multiCurrency: Boolean = bestSellOrders.size > 1,

    val bestSellOrder: ShortOrder?,

    val source: OwnershipSourceDto?,

    val lastUpdatedAt: Instant,

    @Version
    val version: Long? = null
) {

    constructor(shortOwnership: ShortOwnership) : this(
        version = shortOwnership.version,

        blockchain = shortOwnership.blockchain,
        token = shortOwnership.itemId.substringBefore(IdParser.DELIMITER),
        tokenId = shortOwnership.itemId.substringAfter(IdParser.DELIMITER).toBigInteger(),
        owner = shortOwnership.owner,

        bestSellOrders = shortOwnership.bestSellOrders,

        multiCurrency = shortOwnership.multiCurrency,

        bestSellOrder = shortOwnership.bestSellOrder,

        source = shortOwnership.source,

        lastUpdatedAt = shortOwnership.lastUpdatedAt
    )

    fun toShortOwnership(): ShortOwnership {
        return ShortOwnership(
            version = this.version,

            blockchain = this.blockchain,
            itemId = "$token:$tokenId",
            owner = this.owner,

            bestSellOrders = this.bestSellOrders,

            multiCurrency = this.multiCurrency,

            bestSellOrder = this.bestSellOrder,

            lastUpdatedAt = this.lastUpdatedAt,

            source = source
        )
    }

    companion object {

        fun empty(ownershipId: LegacyShortOwnershipId): LegacyShortOwnership {
            return LegacyShortOwnership(
                blockchain = ownershipId.blockchain,
                token = ownershipId.token,
                tokenId = ownershipId.tokenId,
                owner = ownershipId.owner,

                bestSellOrders = emptyMap(),
                bestSellOrder = null,
                lastUpdatedAt = nowMillis(),

                source = null,

                version = null
            )
        }
    }

    fun isNotEmpty(): Boolean {
        return bestSellOrder != null || source != null
    }

    @Transient
    private val _id: LegacyShortOwnershipId = LegacyShortOwnershipId(blockchain, token, tokenId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: LegacyShortOwnershipId
        get() = _id
        set(_) {}

}
