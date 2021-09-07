package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.EthItemDto
import com.rarible.protocol.union.dto.FlowItemDto
import com.rarible.protocol.union.dto.UnionItemDto

object UnionItemContinuationFactory {

    object ByLastUpdatedAndId : ContinuationFactory<UnionItemDto, DateIdContinuation> {
        override fun getContinuation(item: UnionItemDto): DateIdContinuation {
            return DateIdContinuation(item.lastUpdatedAt, getId(item))
        }
    }

    private fun getId(item: UnionItemDto): String {
        return when (item) {
            is EthItemDto -> item.id.value
            is FlowItemDto -> item.id.value
        }
    }

}