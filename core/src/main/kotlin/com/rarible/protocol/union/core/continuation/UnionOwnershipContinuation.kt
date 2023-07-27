package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateIdContinuation

object UnionOwnershipContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOwnership, DateIdContinuation> {
        override fun getContinuation(entity: UnionOwnership): DateIdContinuation {
            return DateIdContinuation(entity.createdAt, entity.id.value)
        }
    }
}
