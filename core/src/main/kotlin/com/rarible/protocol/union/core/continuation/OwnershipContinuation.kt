package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionOwnership

object OwnershipContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOwnership, DateIdContinuation> {
        override fun getContinuation(entity: UnionOwnership): DateIdContinuation {
            return DateIdContinuation(entity.createdAt, entity.id.value)
        }
    }
}