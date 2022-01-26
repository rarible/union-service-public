package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto

object BestOrderValidator {

    fun isValid(order: OrderDto): Boolean {
        return order.status == OrderStatusDto.ACTIVE
    }
}