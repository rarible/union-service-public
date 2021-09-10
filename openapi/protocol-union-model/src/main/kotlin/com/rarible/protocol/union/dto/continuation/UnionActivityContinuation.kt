package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.EthActivityDto
import com.rarible.protocol.union.dto.FlowActivityDto
import com.rarible.protocol.union.dto.UnionActivityDto

object UnionActivityContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<UnionActivityDto, DateIdContinuation> {
        override fun getContinuation(activity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(activity.date, getId(activity))
        }
    }

    private fun getId(activity: UnionActivityDto): String {
        return when (activity) {
            is EthActivityDto -> activity.id.value
            is FlowActivityDto -> activity.id.value
        }
    }

}