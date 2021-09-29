package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.OwnershipDto

object OwnershipContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<OwnershipDto, DateIdContinuation> {
        override fun getContinuation(item: OwnershipDto): DateIdContinuation {
            return DateIdContinuation(item.createdAt, item.id.value)
        }
    }
}