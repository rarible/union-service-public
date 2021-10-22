package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionItem

object ItemContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionItem, DateIdContinuation> {
        override fun getContinuation(entity: UnionItem): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value)
        }
    }
}