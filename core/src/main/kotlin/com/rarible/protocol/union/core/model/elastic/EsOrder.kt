package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadSettings
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

data class EsOrder(
    @Id
    val orderId: String, // blockchain:value

    @Field(type = FieldType.Date)
    val lastUpdatedAt: Instant,

    val type: Type,
    val blockchain: BlockchainDto,
    val platform: PlatformDto,
    val maker: String,
    val make: Asset,
    val makePrice: Double?,
    val makePriceUsd: Double?,
    val taker: String?,
    val take: Asset,
    val takePrice: Double?,
    val takePriceUsd: Double?,
    val start: Instant?,
    val end: Instant?,
    val origins: List<String>, //list of address fulIds
    val status: OrderStatusDto,
) {

    data class Asset(
        val token: String?,
        val tokenId: BigInteger?,
        val isNft: Boolean,
        val value: BigDecimal
    )

    enum class Type {
        SELL, BID
    }

    companion object {
        private const val VERSION = 1

        val ENTITY_DEFINITION = EsEntity.ORDER.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = loadSettings(it)
            )
        }
    }
}
