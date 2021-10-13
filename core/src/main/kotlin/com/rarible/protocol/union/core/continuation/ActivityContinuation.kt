package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.ActivityDto

object ActivityContinuation {

    object ByLastUpdatedAndIdDesc : ContinuationFactory<ActivityDto, DateIdContinuation> {
        override fun getContinuation(entity: ActivityDto): DateIdContinuation {
            return DateIdContinuation(entity.date, entity.id.value, false)
        }
    }

    object ByLastUpdatedAndIdAsc : ContinuationFactory<ActivityDto, DateIdContinuation> {
        override fun getContinuation(entity: ActivityDto): DateIdContinuation {
            return DateIdContinuation(entity.date, entity.id.value, true)
        }
    }
}