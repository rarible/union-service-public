package com.rarible.protocol.union.integration.immutablex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
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

data class ImmutablexMint(
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    val token: Token,
    override val timestamp: Instant,
    val fees: List<ImmutablexFee>?
): ImmutablexEvent(transactionId, timestamp)

data class Token(val type: String, val data: TokenData)

data class TokenData(
    @JsonProperty("token_id")
    val tokenId: String?,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    val properties: ImmutablexDataProperties?,
    val decimals: Int?,
    val quantity: String?,
)

data class ImmutablexTransfersPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexTransfer>,
)

data class ImmutablexTradeAsset(
    val orderId: Long,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("token_id")
    val tokenId: String?,
    @JsonProperty("token_address")
    val tokenAddress: String?,
    val sold: BigInteger,
)


data class ImmutablexDepositsPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexDeposit>,
)

data class ImmutablexWithdrawalPage(
    val cursor: String,
    val remaining: Boolean,
    val result: List<ImmutablexWithdrawal>
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
    val creator: String,
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
    override val timestamp: Instant,
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    val user: String
): ImmutablexEvent(transactionId, timestamp)

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
    override val transactionId: Long,
    @JsonProperty("b")
    val make: TradeSide,
    @JsonProperty("a")
    val take: TradeSide,
    val status: String,
    override val timestamp: Instant
): ImmutablexEvent(transactionId, timestamp)

data class ImmutablexDeposit(
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    val token: Token,
    val status: String,
    override val timestamp: Instant,
    val user: String
): ImmutablexEvent(transactionId, timestamp)

data class ImmutablexWithdrawal(
    val token: Token,
    @JsonProperty("rollup_status")
    val rollupStatus: String,
    val sender: String,
    val status: String,
    @JsonProperty("withdrawn_to_wallet")
    val withdrawnToWallet: Boolean,
    @JsonProperty("transaction_id")
    override val transactionId: Long,
    override val timestamp: Instant,
): ImmutablexEvent(transactionId, timestamp)

//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(JsonSubTypes.Type(value = ImmutablexMint::class),
    JsonSubTypes.Type(value = ImmutablexTransfer::class),
    JsonSubTypes.Type(value = ImmutablexTrade::class),
    JsonSubTypes.Type(value = ImmutablexDeposit::class),
    JsonSubTypes.Type(value = ImmutablexWithdrawal::class))
sealed class ImmutablexEvent(open val transactionId: Long, open val timestamp: Instant): ImmutablexJson

@JsonSubTypes(JsonSubTypes.Type(value = ImmutablexMint::class),
    JsonSubTypes.Type(value = ImmutablexTransfer::class),
    JsonSubTypes.Type(value = ImmutablexTrade::class),
    JsonSubTypes.Type(value = ImmutablexDeposit::class),
    JsonSubTypes.Type(value = ImmutablexWithdrawal::class))
sealed interface ImmutablexJson

class JSONSerde<T: Any> : Serializer<T>, Deserializer<T>, Serde<T> {

    companion object {
        private val OBJ_MAPPER = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
        }
    }

    override fun close() {}

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}

    override fun serialize(topic: String, data: T?): ByteArray? {
        if (data == null) return null

        return try {
            OBJ_MAPPER.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw SerializationException("Error serializing JSON message", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(topic: String, data: ByteArray?): T? {
        if (data == null) return null

        return try {
            OBJ_MAPPER.readValue<Any?>(data) as T?
        } catch (e: Exception) {
            throw SerializationException(e)
        }
    }

    override fun serializer(): Serializer<T> = this

    override fun deserializer(): Deserializer<T> = this
}
