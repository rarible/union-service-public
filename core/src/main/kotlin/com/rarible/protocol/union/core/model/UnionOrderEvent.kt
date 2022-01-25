package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionOrderUpdateEvent::class)
)
sealed class UnionOrderEvent {

    abstract val orderId: OrderIdDto
    abstract val order: OrderDto
}

data class UnionOrderUpdateEvent(
    override val orderId: OrderIdDto,
    override val order: OrderDto
) : UnionOrderEvent() {

    constructor(order: OrderDto) : this(order.id, order)

}