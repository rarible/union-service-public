package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.UnionOrderDto

object UnionOrderContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOrderDto, DateIdContinuation> {
        override fun getContinuation(order: UnionOrderDto): DateIdContinuation {
            return DateIdContinuation(order.lastUpdatedAt, order.id.value)
        }
    }
}