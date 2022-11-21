package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionOrderUpdateEvent::class),
    JsonSubTypes.Type(name = "UPDATE_POOL_NFT", value = UnionPoolNftUpdateEvent::class),
    JsonSubTypes.Type(name = "UPDATE_POOL_ORDER", value = UnionPoolOrderUpdateEvent::class)
)
sealed class UnionOrderEvent {

    abstract val orderId: OrderIdDto
}

data class UnionOrderUpdateEvent(
    override val orderId: OrderIdDto,
    val order: OrderDto
) : UnionOrderEvent() {

    constructor(order: OrderDto) : this(order.id, order)
}

// Event received from blockchains
data class UnionPoolNftUpdateEvent(
    override val orderId: OrderIdDto,
    val inNft: Set<ItemIdDto>,
    val outNft: Set<ItemIdDto>
) : UnionOrderEvent()

// Synthetic event based on inNft/outNft data from UnionPoolNftUpdateEvent
data class UnionPoolOrderUpdateEvent(
    override val orderId: OrderIdDto,
    val order: OrderDto,
    val itemId: ItemIdDto,
    val action: PoolItemAction
) : UnionOrderEvent() {

    constructor(order: OrderDto, itemId: ItemIdDto, action: PoolItemAction) : this(order.id, order, itemId, action)
}

enum class PoolItemAction {
    INCLUDED,
    EXCLUDED,
    UPDATED
}