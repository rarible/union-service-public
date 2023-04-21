package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "MINT", value = UnionMintActivity::class),
    JsonSubTypes.Type(name = "BURN", value = UnionBurnActivity::class),
    JsonSubTypes.Type(name = "TRANSFER", value = UnionTransferActivity::class),
    JsonSubTypes.Type(name = "SWAP", value = UnionOrderMatchSwap::class),
    JsonSubTypes.Type(name = "SELL", value = UnionOrderMatchSell::class),
    JsonSubTypes.Type(name = "BID", value = UnionOrderBidActivity::class),
    JsonSubTypes.Type(name = "LIST", value = UnionOrderListActivity::class),
    JsonSubTypes.Type(name = "CANCEL_BID", value = UnionOrderCancelBidActivity::class),
    JsonSubTypes.Type(name = "CANCEL_LIST", value = UnionOrderCancelListActivity::class),
    JsonSubTypes.Type(name = "AUCTION_OPEN", value = UnionAuctionOpenActivity::class),
    JsonSubTypes.Type(name = "AUCTION_BID", value = UnionAuctionBidActivity::class),
    JsonSubTypes.Type(name = "AUCTION_FINISH", value = UnionAuctionFinishActivity::class),
    JsonSubTypes.Type(name = "AUCTION_CANCEL", value = UnionAuctionCancelActivity::class),
    JsonSubTypes.Type(name = "AUCTION_START", value = UnionAuctionStartActivity::class),
    JsonSubTypes.Type(name = "AUCTION_END", value = UnionAuctionEndActivity::class),
    JsonSubTypes.Type(name = "L2_DEPOSIT", value = UnionL2DepositActivity::class),
    JsonSubTypes.Type(name = "L2_WITHDRAWAL", value = UnionL2WithdrawalActivity::class)
)
sealed class UnionActivity {

    abstract val id: ActivityIdDto
    abstract val date: Instant
    abstract val lastUpdatedAt: Instant?
    abstract val cursor: String?
    abstract val reverted: Boolean?

    /**
     * Returns associated itemId for this activity (if applicable)
     */
    abstract fun itemId(): ItemIdDto?

    /**
     * Returns associated collectionId for this activity (if applicable).
     * If Activity associated with specific itemId, should return null
     */
    open fun collectionId(): CollectionIdDto? = null

    open fun ownershipId(): OwnershipIdDto? = null
    open fun source(): OwnershipSourceDto? = null
    open fun isBlockchainEvent() = false
}

data class UnionMintActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val owner: UnionAddress,
    val contract: ContractAddress? = null,
    val collection: CollectionIdDto?,
    val tokenId: BigInteger? = null,
    val itemId: ItemIdDto? = null,
    val value: BigInteger,
    val mintPrice: BigDecimal? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null
) : UnionActivity() {

    override fun itemId() = itemId
    override fun ownershipId() = this.itemId?.toOwnership(this.owner.value)
    override fun source(): OwnershipSourceDto = OwnershipSourceDto.MINT
    override fun isBlockchainEvent() = this.blockchainInfo != null

}

data class UnionBurnActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val owner: UnionAddress,
    val contract: ContractAddress? = null,
    val collection: CollectionIdDto?,
    val tokenId: BigInteger? = null,
    val itemId: ItemIdDto? = null,
    val value: BigInteger,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null
) : UnionActivity() {

    override fun itemId() = itemId
    override fun isBlockchainEvent() = this.blockchainInfo != null

}

data class UnionTransferActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val from: UnionAddress,
    val owner: UnionAddress,
    val contract: ContractAddress? = null,
    val collection: CollectionIdDto?,
    val tokenId: BigInteger? = null,
    val itemId: ItemIdDto? = null,
    val value: BigInteger,
    val purchase: Boolean? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null
) : UnionActivity() {

    override fun itemId() = itemId
    override fun ownershipId() = this.itemId?.toOwnership(this.owner.value)

    override fun source(): OwnershipSourceDto? {
        return this.purchase?.let {
            if (it) OwnershipSourceDto.PURCHASE else OwnershipSourceDto.TRANSFER
        }
    }

    override fun isBlockchainEvent() = this.blockchainInfo != null
}

sealed class UnionOrderMatchActivity : UnionActivity() {

    abstract val orderId: OrderIdDto?
    abstract val source: OrderActivitySourceDto
    abstract val transactionHash: String
    abstract val blockchainInfo: ActivityBlockchainInfoDto?
}

data class UnionOrderMatchSwap(
    override val orderId: OrderIdDto? = null,
    override val source: OrderActivitySourceDto,
    override val transactionHash: String,
    override val blockchainInfo: ActivityBlockchainInfoDto? = null,
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val left: UnionOrderActivityMatchSideDto,
    val right: UnionOrderActivityMatchSideDto
) : UnionOrderMatchActivity() {

    override fun itemId() = null
}

data class UnionOrderMatchSell(
    override val orderId: OrderIdDto? = null,
    override val source: OrderActivitySourceDto,
    override val transactionHash: String,
    override val blockchainInfo: ActivityBlockchainInfoDto? = null,
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val nft: UnionAsset,
    val payment: UnionAsset,
    val buyer: UnionAddress,
    val seller: UnionAddress,
    val buyerOrderHash: String? = null,
    val sellerOrderHash: String? = null,
    val price: BigDecimal,
    val priceUsd: BigDecimal? = null,
    val amountUsd: BigDecimal? = null,
    val type: Type,
    val sellMarketplaceMarker: String? = null,
    val buyMarketplaceMarker: String? = null
) : UnionOrderMatchActivity() {

    enum class Type {
        SELL,
        ACCEPT_BID
    }

    override fun itemId() = this.nft.type.itemId()
    override fun collectionId() = this.nft.type.collectionId()
    override fun isBlockchainEvent() = this.blockchainInfo != null

}

data class UnionOrderBidActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAsset,
    val take: UnionAsset,
    val price: BigDecimal,
    val priceUsd: BigDecimal? = null,
    val source: OrderActivitySourceDto? = null,
    val marketplaceMarker: String? = null
) : UnionActivity() {

    override fun itemId() = this.take.type.itemId()
    override fun collectionId() = this.take.type.collectionId()
}

data class UnionOrderListActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAsset,
    val take: UnionAsset,
    val price: BigDecimal,
    val priceUsd: BigDecimal? = null,
    val source: OrderActivitySourceDto? = null
) : UnionActivity() {

    override fun itemId() = this.make.type.itemId()
    override fun collectionId() = this.make.type.collectionId()
}

data class UnionOrderCancelBidActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAssetType,
    val take: UnionAssetType,
    val source: OrderActivitySourceDto? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null
) : UnionActivity() {

    override fun itemId() = this.take.itemId()
    override fun collectionId() = this.take.collectionId()
    override fun isBlockchainEvent() = this.blockchainInfo != null
}

data class UnionOrderCancelListActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAssetType,
    val take: UnionAssetType,
    val source: OrderActivitySourceDto? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null
) : UnionActivity() {

    override fun itemId() = this.make.itemId()
    override fun collectionId() = this.make.collectionId()
    override fun isBlockchainEvent() = this.blockchainInfo != null
}

data class UnionAuctionOpenActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val auction: AuctionDto,
    val transactionHash: String
) : UnionActivity() {

    override fun itemId() = this.auction.getItemId()

}

data class UnionAuctionBidActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val auction: AuctionDto,
    val bid: AuctionBidDto,
    val transactionHash: String
) : UnionActivity() {

    override fun itemId() = this.auction.getItemId()
    override fun ownershipId() = null

}

data class UnionAuctionFinishActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val auction: AuctionDto,
    val transactionHash: String
) : UnionActivity() {

    override fun itemId() = this.auction.getItemId()

}

data class UnionAuctionCancelActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val auction: AuctionDto,
    val transactionHash: String
) : UnionActivity() {

    override fun itemId() = this.auction.getItemId()

}

data class UnionAuctionStartActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val auction: AuctionDto
) : UnionActivity() {

    override fun itemId() = this.auction.getItemId()

}

data class UnionAuctionEndActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val auction: AuctionDto
) : UnionActivity() {

    override fun itemId() = this.auction.getItemId()

}

data class UnionL2DepositActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val user: UnionAddress,
    val status: String,
    val itemId: ItemIdDto,
    val collection: CollectionIdDto?,
    val value: BigInteger? = null
) : UnionActivity() {

    override fun itemId() = this.itemId
}

data class UnionL2WithdrawalActivity(
    override val id: ActivityIdDto,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val cursor: String? = null,
    override val reverted: Boolean? = null,
    val user: UnionAddress,
    val status: String,
    val itemId: ItemIdDto,
    val collection: CollectionIdDto?,
    val value: BigInteger? = null
) : UnionActivity() {

    override fun itemId() = this.itemId

}

data class UnionOrderActivityMatchSideDto(
    val maker: UnionAddress,
    val hash: String? = null,
    val asset: UnionAsset
)