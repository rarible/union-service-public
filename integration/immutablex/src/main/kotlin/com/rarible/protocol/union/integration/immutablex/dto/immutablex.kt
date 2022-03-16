package com.rarible.protocol.union.integration.immutablex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.math.BigDecimal
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
    val metadata: Map<String, Any>,
    val name: String?,
    val status: String?,
    @JsonProperty("token_address")
    val tokenAddress: String,
    @JsonProperty("token_id")
    val tokenId: Long,
    val uri: String?,
    @JsonProperty("updated_at")
    val updatedAt: Instant?,
    val user: String?
)

data class ImmutablexCollectionShort(
    @JsonProperty("icon_url")
    val iconUrl: String?,
    val name: String?
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ImmutablexCollection(
    val address: String,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val collectionImageUrl: String,
    val projectId: Long,
    val metadataApiUrl: String
)

data class ImmutablexFee(val address: String, val percentage: BigDecimal, val type: String)

data class ImmutablexOrderFee(val address: String, val amount: BigDecimal, val token: FeeToken, val type: String)

data class FeeToken(val type: String, val data: FeeTokenData)

data class FeeTokenData(
    @JsonProperty("contract_address")
    val contractAddress: String?,
    val decimals: Int
)

data class ImmutablexAssetsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexAsset>
)

data class ImmutablexPage<T>(
    val cursor: String,
    val remaining: Boolean,
    val result: List<T>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImmutablexMint(
    val transactionId: Long,
    val token: Token,
    val user: String,
    val timestamp: Instant,
    val fees: List<ImmutablexFee>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Token(val type: String, val data: TokenData)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenData(
    @JsonProperty("token_id")
    val tokenId: String?,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    val properties: ImmutablexDataProperties?,
    val decimals: Int?,
    val quantity: String?,
)

data class ImmutablexMintsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexMint>
)

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
    val creator: String
)

data class ImmutablexOrderSide(
    val data: ImmutablexOrderData,
    val type: String
)

data class ImmutablexOrderData(
    val decimals: Int,
    val id: String?,
    val quantity: String,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    @JsonProperty("token_id")
    val tokenId: String?,
    val properties: ImmutablexDataProperties?
)

data class ImmutablexDataProperties(
    val name: String?,
    val imageUrl: String?,
    val collection: ImmutablexCollectionShort
)

data class ImmutablexOrdersPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexOrder>
)

data class ImmutablexTransfer(
    val token: Token,
    val receiver: String,
    val status: String,
    val timestamp: Instant,
    @JsonProperty("transaction_id")
    val transactionId: Long
)

data class TradeSide(
    @JsonProperty("order_id")
    val orderId: Long,
    val sold: BigDecimal,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    @JsonProperty("token_id")
    val tokenId: String?,
    @JsonProperty("token_type")
    val tokenType: String?
)

data class ImmutablexTrade(
    @JsonProperty("transaction_id")
    val transactionId: Long,
    @JsonProperty("b")
    val make: TradeSide,
    @JsonProperty("a")
    val take: TradeSide,
    val status: String,
    val timestamp: Instant
)

data class ImmutablexDeposit(
    @JsonProperty("transaction_id")
    val transactionId: Long,
    val token: Token,
    val status: String,
    val timestamp: Instant,
    val user: String
)

data class ImmutablexWithdrawal(
    @JsonProperty("transaction_id")
    val transactionId: Long,
    val token: Token,
    @JsonProperty("rollup_status")
    val rollupStatus: String,
    val sender: String,
    val status: String,
    val timestamp: Instant,
    @JsonProperty("withdrawn_to_wallet")
    val withdrawnToWallet: Boolean
)
