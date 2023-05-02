package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.IdParser
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@Document("enrichment_activity")
sealed class EnrichmentActivity {
    abstract val id: EnrichmentActivityId
    abstract val blockchain: BlockchainDto
    abstract val activityId: String
    abstract val activityType: ActivityTypeDto

    abstract val itemId: ItemIdDto?
    abstract val collection: CollectionIdDto?
    abstract val contract: ContractAddress?

    abstract val date: Instant
    abstract val lastUpdatedAt: Instant?
}

data class EnrichmentMintActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val owner: UnionAddress,
    override val contract: ContractAddress? = null,
    override val collection: CollectionIdDto? = null,
    val tokenId: BigInteger? = null,
    override val itemId: ItemIdDto,
    val value: BigInteger,
    val mintPrice: BigDecimal? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.MINT
        set(_) {}
}

data class EnrichmentBurnActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val owner: UnionAddress,
    override val contract: ContractAddress? = null,
    override val collection: CollectionIdDto? = null,
    val tokenId: BigInteger? = null,
    override val itemId: ItemIdDto,
    val value: BigInteger,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.BURN
        set(_) {}
}

data class EnrichmentTransferActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val from: UnionAddress,
    val owner: UnionAddress,
    override val contract: ContractAddress? = null,
    override val collection: CollectionIdDto? = null,
    val tokenId: BigInteger? = null,
    override val itemId: ItemIdDto,
    val value: BigInteger,
    val purchase: Boolean? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.TRANSFER
        set(_) {}
}

sealed class EnrichmentOrderMatchActivity : EnrichmentActivity() {

    abstract val orderId: OrderIdDto?
    abstract val source: OrderActivitySourceDto
    abstract val transactionHash: String
    abstract val blockchainInfo: ActivityBlockchainInfoDto?
}

data class EnrichmentOrderMatchSwap(
    override val orderId: OrderIdDto? = null,
    override val source: OrderActivitySourceDto,
    override val transactionHash: String,
    override val blockchainInfo: ActivityBlockchainInfoDto? = null,
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    override val contract: ContractAddress? = null,
    override val collection: CollectionIdDto? = null,
    override val itemId: ItemIdDto? = null,
    val left: EnrichmentOrderActivityMatchSide,
    val right: EnrichmentOrderActivityMatchSide,
) : EnrichmentOrderMatchActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.TRANSFER
        set(_) {}
}

data class EnrichmentOrderMatchSell(
    override val orderId: OrderIdDto? = null,
    override val source: OrderActivitySourceDto,
    override val transactionHash: String,
    override val blockchainInfo: ActivityBlockchainInfoDto? = null,
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
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
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
    val sellMarketplaceMarker: String? = null,
    val buyMarketplaceMarker: String? = null,
) : EnrichmentOrderMatchActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.SELL
        set(_) {}

    enum class Type {
        SELL,
        ACCEPT_BID
    }
}

data class EnrichmentOrderBidActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAsset,
    val take: UnionAsset,
    val price: BigDecimal,
    val priceUsd: BigDecimal? = null,
    val source: OrderActivitySourceDto? = null,
    val marketplaceMarker: String? = null,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto? = null,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.BID
        set(_) {}
}

data class EnrichmentOrderListActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAsset,
    val take: UnionAsset,
    val price: BigDecimal,
    val priceUsd: BigDecimal? = null,
    val source: OrderActivitySourceDto? = null,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.LIST
        set(_) {}
}

data class EnrichmentOrderCancelBidActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAssetType,
    val take: UnionAssetType,
    val source: OrderActivitySourceDto? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto? = null,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.CANCEL_BID
        set(_) {}
}

data class EnrichmentOrderCancelListActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val orderId: OrderIdDto? = null,
    val hash: String,
    val maker: UnionAddress,
    val make: UnionAssetType,
    val take: UnionAssetType,
    val source: OrderActivitySourceDto? = null,
    val transactionHash: String,
    val blockchainInfo: ActivityBlockchainInfoDto? = null,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.CANCEL_LIST
        set(_) {}
}

data class EnrichmentAuctionOpenActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val auction: AuctionDto,
    val transactionHash: String,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.AUCTION_CREATED
        set(_) {}
}

data class EnrichmentAuctionBidActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val auction: AuctionDto,
    val bid: AuctionBidDto,
    val transactionHash: String,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.AUCTION_BID
        set(_) {}
}

data class EnrichmentAuctionFinishActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val auction: AuctionDto,
    val transactionHash: String,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}


    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.AUCTION_FINISHED
        set(_) {}
}

data class EnrichmentAuctionCancelActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val auction: AuctionDto,
    val transactionHash: String,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.AUCTION_CANCEL
        set(_) {}
}

data class EnrichmentAuctionStartActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val auction: AuctionDto,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.AUCTION_STARTED
        set(_) {}
}

data class EnrichmentAuctionEndActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val auction: AuctionDto,
    override val contract: ContractAddress? = null,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.AUCTION_ENDED
        set(_) {}
}

data class EnrichmentL2DepositActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val user: UnionAddress,
    val status: String,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
    override val contract: ContractAddress? = null,
    val value: BigInteger? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.TRANSFER
        set(_) {}
}

data class EnrichmentL2WithdrawalActivity(
    override val blockchain: BlockchainDto,
    override val activityId: String,
    override val date: Instant,
    override val lastUpdatedAt: Instant? = null,
    val user: UnionAddress,
    val status: String,
    override val itemId: ItemIdDto,
    override val collection: CollectionIdDto? = null,
    override val contract: ContractAddress? = null,
    val value: BigInteger? = null,
) : EnrichmentActivity() {
    @Transient
    private val _id: EnrichmentActivityId = EnrichmentActivityId(blockchain, activityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: EnrichmentActivityId
        get() = _id
        set(_) {}

    @get:AccessType(AccessType.Type.PROPERTY)
    override var activityType: ActivityTypeDto
        get() = ActivityTypeDto.TRANSFER
        set(_) {}
}

data class EnrichmentOrderActivityMatchSide(
    val maker: UnionAddress,
    val hash: String? = null,
    val asset: UnionAsset
)

data class EnrichmentActivityId(
    val blockchain: BlockchainDto,
    val activityId: String
) {

    constructor(dto: ActivityIdDto) : this(
        dto.blockchain,
        dto.value
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): ActivityIdDto {
        return ActivityIdDto(
            blockchain = blockchain,
            value = activityId
        )
    }

    companion object {

        fun of(activityId: String): EnrichmentActivityId {
            return EnrichmentActivityId(IdParser.parseActivityId(activityId))
        }
    }
}