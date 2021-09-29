package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.UnionActivityDto

object UnionActivityContinuation {

    object ByLastUpdatedAndIdDesc : ContinuationFactory<UnionActivityDto, DateIdContinuation> {
        override fun getContinuation(activity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(activity.date, activity.id.value, false)
        }
    }

    object ByLastUpdatedAndIdAsc : ContinuationFactory<UnionActivityDto, DateIdContinuation> {
        override fun getContinuation(activity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(activity.date, activity.id.value, true)
        }
    }
}