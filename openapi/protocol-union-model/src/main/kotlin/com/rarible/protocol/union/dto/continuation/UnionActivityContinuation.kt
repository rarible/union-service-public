package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.ActivityDto

object ActivityContinuation {

    object ByLastUpdatedAndIdDesc : ContinuationFactory<ActivityDto, DateIdContinuation> {
        override fun getContinuation(activity: ActivityDto): DateIdContinuation {
            return DateIdContinuation(activity.date, activity.id.value, false)
        }
    }

    object ByLastUpdatedAndIdAsc : ContinuationFactory<ActivityDto, DateIdContinuation> {
        override fun getContinuation(activity: ActivityDto): DateIdContinuation {
            return DateIdContinuation(activity.date, activity.id.value, true)
        }
    }
}