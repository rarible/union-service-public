package com.rarible.protocol.union.integration.immutablex.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

data class ImmutablexAsset(
    val collection: ImmutablexCollectionShort,
    @JsonProperty("created_at")
    val createdAt: Instant?,
    val description: String?,
    val fees: List<ImmutablexFee> = emptyList(),
    val id: String?,
    @JsonProperty("image_url")
    val imageUrl: String?,
    val metadata: Map<String, Any>?,
    val name: String?,
    val status: String?,
    @JsonProperty("token_address")
    val tokenAddress: String,
    @JsonProperty("token_id")
    private val tokenId: String,
    val uri: String?,
    @JsonProperty("updated_at")
    val updatedAt: Instant?,
    val user: String?,
) {

    val itemId = "$tokenAddress:$tokenId"

    fun encodedTokenId() = TokenIdDecoder.encode(tokenId)

    fun encodedItemId() = "$tokenAddress:${encodedTokenId()}"
}

data class ImmutablexCollectionShort(
    @JsonProperty("icon_url")
    val iconUrl: String?,
    val name: String?,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ImmutablexCollection(
    val address: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val collectionImageUrl: String?,
    val projectId: Long,
    val projectOwnerAddress: String?,
    val metadataApiUrl: String,
)

data class ImmutablexFee(val address: String, val percentage: BigDecimal, val type: String)

data class ImmutablexOrderFee(val address: String, val amount: BigDecimal, val token: FeeToken, val type: String)

data class FeeToken(val type: String, val data: FeeTokenData)

data class FeeTokenData(
    @JsonProperty("contract_address")
    val contractAddress: String?,
    val decimals: Int,
)

data class ImmutablexAssetsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexAsset>,
)

data class ImmutablexPage<T>(
    val cursor: String,
    val remaining: Boolean,
    val result: List<T>,
)

data class ImmutablexMint(
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    override val token: Token,
    val user: String,
    override val timestamp: Instant,
    val fees: List<ImmutablexFee>?,
    val status: String?,
) : ImmutablexTokenEvent(transactionId, timestamp, token)

data class Token(val type: String, val data: TokenData)

data class TokenData(
    @JsonProperty("token_id")
    private val tokenId: String,
    @JsonProperty("token_address")
    val tokenAddress: String,
    val properties: ImmutablexDataProperties?,
    val decimals: Int?,
    val quantity: BigInteger,
    val id: String?,
) {

    fun itemId() = "$tokenAddress:$tokenId"

    fun encodedTokenId() = TokenIdDecoder.encode(tokenId)

    fun encodedItemId() = "$tokenAddress:${encodedTokenId()}"
}

data class ImmutablexMintsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexMint>,
) {

    companion object {

        val EMPTY = ImmutablexMintsPage("", false, emptyList())
    }
}

data class ImmutablexOrder(
    @JsonProperty("order_id")
    val orderId: Long,
    @JsonProperty("amount_sold")
    val amountSold: String?,
    val buy: ImmutablexOrderSide,
    @JsonProperty("expiration_timestamp")
    val expirationTimestamp: Instant,
    val fees: List<ImmutablexOrderFee>?,
    val sell: ImmutablexOrderSide,
    val status: String,
    @JsonProperty("timestamp")
    val createdAt: Instant,
    @JsonProperty("updated_timestamp")
    val updatedAt: Instant?,
    @JsonProperty("user")
    val creator: String,
)

data class ImmutablexOrderSide(
    val data: ImmutablexOrderData,
    val type: String,
)

data class ImmutablexOrderData(
    val decimals: Int,
    val id: String?,
    val quantity: BigInteger,
    @JsonProperty("quantity_with_fees")
    private val quantityWithFees: String?,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    @JsonProperty("token_id")
    private val tokenId: String?,
    val properties: ImmutablexDataProperties?,
) {

    fun itemId(): String = "${tokenAddress}:${tokenId}"

    fun encodedTokenId() = tokenId?.let { TokenIdDecoder.encode(tokenId) }

    fun getQuantityWithFees(): BigInteger {
        return if (quantityWithFees.isNullOrBlank()) {
            quantity
        } else {
            BigInteger(quantityWithFees)
        }
    }
}

data class ImmutablexDataProperties(
    val name: String?,
    @JsonProperty("image_url")
    val imageUrl: String?,
    val collection: ImmutablexCollectionShort,
)

data class ImmutablexOrdersPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexOrder>,
) {

    constructor(result: List<ImmutablexOrder>) : this(
        "",
        false,
        result
    )

    companion object {

        fun empty() = ImmutablexOrdersPage(emptyList())
    }
}

data class ImmutablexTransfer(
    override val token: Token,
    val receiver: String,
    val status: String,
    override val timestamp: Instant,
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    val user: String,
) : ImmutablexTokenEvent(transactionId, timestamp, token) {

    val isBurn = receiver == ZERO_ADDRESS

    companion object {

        val ZERO_ADDRESS = Address.ZERO().prefixed()
    }
}

data class ImmutablexTransfersPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexTransfer>,
) {

    companion object {

        val EMPTY = ImmutablexTransfersPage("", false, emptyList())
    }
}

data class TradeSide(
    @JsonProperty("order_id")
    val orderId: Long,
    val sold: BigDecimal,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    @JsonProperty("token_id")
    private val tokenId: String?,
    @JsonProperty("token_type")
    val tokenType: String?,
) {

    fun encodedTokenId() = TokenIdDecoder.encode(tokenId!!) // TODO what if null?
}

data class ImmutablexTrade(
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    @JsonProperty("b")
    val make: TradeSide,
    @JsonProperty("a")
    val take: TradeSide,
    val status: String,
    override val timestamp: Instant,
) : ImmutablexEvent(transactionId, timestamp)

data class ImmutablexTradesPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexTrade>,
) {

    companion object {

        val EMPTY = ImmutablexTradesPage("", false, emptyList())
    }
}

data class ImmutablexDeposit(
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    override val token: Token,
    val status: String,
    override val timestamp: Instant,
    val user: String,
) : ImmutablexTokenEvent(transactionId, timestamp, token)

data class ImmutablexWithdrawal(
    override val token: Token,
    @JsonProperty("rollup_status")
    val rollupStatus: String,
    val sender: String,
    val status: String,
    @JsonProperty("withdrawn_to_wallet")
    val withdrawnToWallet: Boolean,
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    override val timestamp: Instant,
) : ImmutablexTokenEvent(transactionId, timestamp, token)

sealed class ImmutablexTokenEvent(transactionId: Long, timestamp: Instant, open val token: Token) :
    ImmutablexEvent(transactionId, timestamp) {

    fun itemId(): String = token.data.itemId()

    fun encodedItemId(): String = token.data.encodedItemId()
}

@JsonSubTypes(
    JsonSubTypes.Type(value = ImmutablexMint::class),
    JsonSubTypes.Type(value = ImmutablexTransfer::class),
    JsonSubTypes.Type(value = ImmutablexTrade::class),
    JsonSubTypes.Type(value = ImmutablexDeposit::class),
    JsonSubTypes.Type(value = ImmutablexWithdrawal::class)
)
sealed class ImmutablexEvent(open val transactionId: Long, open val timestamp: Instant) : ImmutablexJson {

    val activityId
        get() = ActivityIdDto(BlockchainDto.IMMUTABLEX, transactionId.toString())
}

@JsonSubTypes(
    JsonSubTypes.Type(value = ImmutablexMint::class),
    JsonSubTypes.Type(value = ImmutablexTransfer::class),
    JsonSubTypes.Type(value = ImmutablexTrade::class),
    JsonSubTypes.Type(value = ImmutablexDeposit::class),
    JsonSubTypes.Type(value = ImmutablexWithdrawal::class)
)
sealed interface ImmutablexJson

