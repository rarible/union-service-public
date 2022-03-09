package com.rarible.protocol.union.integration.immutablex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant


data class ImmutablexAsset(
    val collection: ImmutablexCollection,
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

data class ImmutablexCollection(
    @JsonProperty("icon_url")
    val iconUrl: String?,
    val name: String?
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImmutableMint(
    val transactionId: Long,
    val token: Token,
    val user: String,
    val timestamp: Instant
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Token(val type: String, val data: TokenData)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenData(
    @JsonProperty("token_id")
    val tokenId: String?,
    @JsonProperty("token_address")
    val tokenAddress: String?
)

data class ImmutablexMintsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutableMint>
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
    val properties: ImmutablexOrderDataProperties?
)

data class ImmutablexOrderDataProperties(
    val name: String?,
    val imageUrl: String?,
    val collection: ImmutablexCollection
)
