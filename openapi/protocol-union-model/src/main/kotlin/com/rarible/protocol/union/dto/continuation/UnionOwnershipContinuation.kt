package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.UnionOwnershipDto

object UnionOwnershipContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOwnershipDto, DateIdContinuation> {
        override fun getContinuation(item: UnionOwnershipDto): DateIdContinuation {
            return DateIdContinuation(item.createdAt, item.id.value)
        }
    }
}