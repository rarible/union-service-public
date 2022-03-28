package com.rarible.domain

import com.rarible.core.common.Identifiable
import com.rarible.marketplace.core.model.Blockchain
import com.rarible.marketplace.core.model.BlockchainAddress
import com.rarible.marketplace.core.model.toBlockchainAddress
import com.rarible.protocol.union.core.domain.Auction
import com.rarible.protocol.union.core.domain.BlockchainId
import com.rarible.protocol.union.core.domain.Item
import com.rarible.protocol.union.core.domain.ItemHistory
import com.rarible.protocol.union.core.domain.constant.PlatformType
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.Date

const val OWNERSHIP_COLLECTION = "ownership"

@Document(OWNERSHIP_COLLECTION)
data class Ownership(
    @Id
    override val id: String,
    val token: BlockchainAddress,
    val tokenId: String,
    val itemId: String? = Item.getId(token, tokenId),
    val owner: BlockchainAddress,

    /** Сколько айтемов находится у данного владельца. */
    val value: BigDecimal,
    /** Сколько айтемов из общего количества ([value]), находящихся во владении данного пользователя, являются лейзи. */
    val lazyValue: BigDecimal = ZERO,
    val pending: List<ItemHistory> = emptyList(),
    val price: BigDecimal? = null,
    val priceEth: BigDecimal? = null,
    val buyToken: BlockchainAddress? = null,
    val buyTokenId: String? = null,
    /** Сколько айтемов этого владельца было выставлено на продажу изначально. */
    val selling: BigDecimal = ZERO,
    /** Сколько айтемов уже подано. */
    val sold: BigDecimal = ZERO,
    /** Сколько айтемов этого владельца сейчас находятся на продаже. */
    val stock: BigDecimal = ZERO,
    /** Платформа, на которой был создан "лучший" ордер на продажу айтема данного владельца. */
    val platform: PlatformType? = null,
    val status: Status = Status.NOT_FOR_SALE,
    val date: Date,

    val hide: Boolean = false,
    val pin: Date? = null,
    val categories: Set<String> = emptySet(),
    val verified: Boolean = false,
    val blacklisted: Boolean = false,
    val likes: Long = 0,
    val auction: Auction? = null,

    val blockchain: Blockchain = token.blockchain,
    @Version
    val version: Long? = null,
    @CreatedDate
    val createdDate: Date? = null,
) : Identifiable<String> {

    var statusSort: Int
        @AccessType(AccessType.Type.PROPERTY)
        get() = status.sort
        set(statusSort: Int) {}

    companion object {
        fun parseId(id: String): Triple<BlockchainAddress, String, BlockchainAddress> {
            val parts = id.split(":")
            if (parts.size < 3) {
                throw IllegalArgumentException("Incorrect format of ownershipId: $id")
            }
            return Triple(parts[0].toBlockchainAddress(), parts[1], parts[2].toBlockchainAddress())
        }

        fun getId(token: BlockchainAddress, tokenId: String, owner: BlockchainAddress): String =
            "$token:$tokenId:$owner"

        fun getId(itemId: BlockchainId, owner: BlockchainAddress): String = "$itemId:$owner"

        fun empty(token: BlockchainAddress, tokenId: String, owner: BlockchainAddress): Ownership =
            Ownership(
                id = getId(token, tokenId, owner),
                token = token,
                tokenId = tokenId,
                owner = owner,
                value = ZERO,
                date = Date()
            )
    }

    enum class Status(val sort: Int) {
        NOT_FOR_SALE(sort = 4),
        OPEN_FOR_OFFERS(sort = 3),
        FIXED_PRICE(sort = 1),
        AUCTION(sort = 2)
    }
}
