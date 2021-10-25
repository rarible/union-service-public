package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto

sealed class UnionOrderEvent {

    abstract val orderId: OrderIdDto
}

data class UnionOrderUpdateEvent(
    override val orderId: OrderIdDto,
    val order: OrderDto
) : UnionOrderEvent() {

    constructor(order: OrderDto) : this(order.id, order)

}