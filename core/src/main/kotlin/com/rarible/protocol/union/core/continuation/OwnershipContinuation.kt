package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.UnionOwnershipDto

object OwnershipContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOwnershipDto, DateIdContinuation> {
        override fun getContinuation(entity: UnionOwnershipDto): DateIdContinuation {
            return DateIdContinuation(entity.createdAt, entity.id.value)
        }
    }
}