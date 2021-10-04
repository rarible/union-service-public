package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.OrderDto

object OrderContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<OrderDto, DateIdContinuation> {
        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value)
        }
    }
}