package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionOrderUpdateEvent::class),
    JsonSubTypes.Type(name = "UPDATE_AMM_NFT", value = UnionAmmOrderNftUpdateEvent::class),
    JsonSubTypes.Type(name = "UPDATE_AMM", value = UnionAmmOrderUpdateEvent::class)
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
data class UnionAmmOrderNftUpdateEvent(
    override val orderId: OrderIdDto,
    val inNft: List<ItemIdDto>,
    val outNft: List<ItemIdDto>
) : UnionOrderEvent()

// Synthetic event based on inNft/outNft data from UnionAmmOrderNftUpdateEvent
data class UnionAmmOrderUpdateEvent(
    override val orderId: OrderIdDto,
    val order: OrderDto,
    val itemId: ItemIdDto
) : UnionOrderEvent() {

    constructor(order: OrderDto, itemId: ItemIdDto) : this(order.id, order, itemId)
}