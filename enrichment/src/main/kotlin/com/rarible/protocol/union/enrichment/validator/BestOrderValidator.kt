package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto

object BestOrderValidator {

    fun isValid(order: UnionOrder): Boolean {
        return order.status == UnionOrder.Status.ACTIVE && order.taker == null
    }

    fun isValid(order: OrderDto): Boolean {
        return order.status == OrderStatusDto.ACTIVE && order.taker == null
    }
}
