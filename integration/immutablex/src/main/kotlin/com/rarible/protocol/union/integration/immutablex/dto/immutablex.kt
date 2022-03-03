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
    val tokenId: String,
    @JsonProperty("token_address")
    val tokenAddress: String
)

data class ImmutablexMintsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutableMint>
)
