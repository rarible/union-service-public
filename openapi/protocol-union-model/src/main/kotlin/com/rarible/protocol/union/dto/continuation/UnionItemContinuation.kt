package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.ItemDto

object ItemContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<ItemDto, DateIdContinuation> {
        override fun getContinuation(item: ItemDto): DateIdContinuation {
            return DateIdContinuation(item.lastUpdatedAt, item.id.value)
        }
    }
}