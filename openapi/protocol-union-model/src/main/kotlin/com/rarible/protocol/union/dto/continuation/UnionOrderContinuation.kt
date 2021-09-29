package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.OrderDto

object OrderContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<OrderDto, DateIdContinuation> {
        override fun getContinuation(order: OrderDto): DateIdContinuation {
            return DateIdContinuation(order.lastUpdatedAt, order.id.value)
        }
    }
}