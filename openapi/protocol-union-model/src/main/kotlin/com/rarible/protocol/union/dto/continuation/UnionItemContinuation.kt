package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.UnionItemDto

object UnionItemContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionItemDto, DateIdContinuation> {
        override fun getContinuation(item: UnionItemDto): DateIdContinuation {
            return DateIdContinuation(item.lastUpdatedAt, item.id.value)
        }
    }
}