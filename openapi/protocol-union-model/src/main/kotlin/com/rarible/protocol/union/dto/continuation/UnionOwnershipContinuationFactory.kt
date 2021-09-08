package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.EthOwnershipDto
import com.rarible.protocol.union.dto.FlowOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipDto

object UnionOwnershipContinuationFactory {

    object ByLastUpdatedAndId : ContinuationFactory<UnionOwnershipDto, DateIdContinuation> {
        override fun getContinuation(item: UnionOwnershipDto): DateIdContinuation {
            return DateIdContinuation(item.createdAt, getId(item))
        }
    }

    private fun getId(item: UnionOwnershipDto): String {
        return when (item) {
            is EthOwnershipDto -> item.id.value
            is FlowOwnershipDto -> item.id.value
        }
    }

}