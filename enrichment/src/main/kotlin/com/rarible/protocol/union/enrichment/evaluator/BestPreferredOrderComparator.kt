package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortOrder

class BestPreferredOrderComparator(
    private val comparator: BestOrderComparator
) : BestOrderComparator {

    override val name: String = "BestPreferredOrder"

    override fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder {
        val isCurrentPreferred = isPreferred(current)
        val isUpdatedPreferred = isPreferred(updated)

        val bestOrder = if (isCurrentPreferred != isUpdatedPreferred) {
            // if one of orders has preferred type and second hasn't return select preferred Order
            if (isCurrentPreferred) current else updated
        } else {
            // If both orders has preferred type or both are not preferred, comparing them
            comparator.compare(current, updated)
        }
        return bestOrder

    }

    companion object {

        fun isPreferred(order: ShortOrder): Boolean {
            return order.platform == PlatformDto.RARIBLE.name
        }
    }

}