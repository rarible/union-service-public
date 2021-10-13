package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.UnionItemDto

object ItemContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionItemDto, DateIdContinuation> {
        override fun getContinuation(entity: UnionItemDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value)
        }
    }
}