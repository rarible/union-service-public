package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.EthOrderDto
import com.rarible.protocol.union.dto.FlowOrderDto
import com.rarible.protocol.union.dto.UnionOrderDto

object UnionOrderContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOrderDto, DateIdContinuation> {
        override fun getContinuation(order: UnionOrderDto): DateIdContinuation {
            return DateIdContinuation(order.lastUpdatedAt, getId(order))
        }
    }

    private fun getId(order: UnionOrderDto): String {
        return when (order) {
            is EthOrderDto -> order.id.value
            is FlowOrderDto -> order.id.value
        }
    }

}