package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigDecimal
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "CANCEL", value = UnionPendingOrderCancel::class),
    JsonSubTypes.Type(name = "ORDER_SIDE_MATCH", value = UnionPendingOrderMatch::class),
    JsonSubTypes.Type(name = "ON_CHAIN_ORDER", value = UnionOnChainOrder::class),
    JsonSubTypes.Type(name = "ON_CHAIN_AMM_ORDER", value = UnionOnChainAmmOrder::class)
)
sealed class UnionPendingOrder {

    abstract val id: OrderIdDto
    abstract val make: UnionAsset?
    abstract val take: UnionAsset?
    abstract val date: Instant
    abstract val maker: UnionAddress?
}

data class UnionPendingOrderCancel(
    override val id: OrderIdDto,
    override val make: UnionAsset? = null,
    override val take: UnionAsset? = null,
    override val date: Instant,
    override val maker: UnionAddress? = null,
    val owner: UnionAddress? = null
) : UnionPendingOrder()

data class UnionPendingOrderMatch(
    override val id: OrderIdDto,
    override val make: UnionAsset? = null,
    override val take: UnionAsset? = null,
    override val date: Instant,
    override val maker: UnionAddress? = null,
    val side: Side? = null,
    val fill: BigDecimal,
    val taker: UnionAddress? = null,
    val counterHash: String? = null,
    val makeUsd: BigDecimal? = null,
    val takeUsd: BigDecimal? = null,
    val makePriceUsd: BigDecimal? = null,
    val takePriceUsd: BigDecimal? = null
) : UnionPendingOrder() {

    enum class Side {
        LEFT,
        RIGHT
    }
}

data class UnionOnChainOrder(
    override val id: OrderIdDto,
    override val make: UnionAsset? = null,
    override val take: UnionAsset? = null,
    override val date: Instant,
    override val maker: UnionAddress? = null
) : UnionPendingOrder()

data class UnionOnChainAmmOrder(
    override val id: OrderIdDto,
    override val make: UnionAsset? = null,
    override val take: UnionAsset? = null,
    override val date: Instant,
    override val maker: UnionAddress? = null
) : UnionPendingOrder()
