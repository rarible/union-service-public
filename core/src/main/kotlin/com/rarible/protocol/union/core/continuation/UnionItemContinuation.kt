package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateIdContinuation

object UnionItemContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionItem, DateIdContinuation> {
        override fun getContinuation(entity: UnionItem): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value)
        }
    }
}