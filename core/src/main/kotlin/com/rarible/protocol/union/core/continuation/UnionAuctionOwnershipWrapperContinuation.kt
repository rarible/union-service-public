package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionAuctionOwnershipWrapper
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateIdContinuation

object UnionAuctionOwnershipWrapperContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionAuctionOwnershipWrapper, DateIdContinuation> {
        override fun getContinuation(entity: UnionAuctionOwnershipWrapper): DateIdContinuation {
            return DateIdContinuation(entity.date, entity.ownershipId.value)
        }
    }
}