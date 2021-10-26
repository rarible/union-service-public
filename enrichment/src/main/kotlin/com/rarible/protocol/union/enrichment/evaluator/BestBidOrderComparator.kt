package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder

object BestBidOrderComparator : BestOrderComparator {

    override val name: String = "BestBidOrder"

    override fun compare(
        current: ShortOrder,
        updated: ShortOrder
    ): ShortOrder {
        val currentMakePrice = current.makePrice
        val updatedMakePrice = updated.makePrice

        val isCurrentMakePriceLesser = when {
            currentMakePrice == null -> true
            updatedMakePrice != null -> currentMakePrice <= updatedMakePrice
            else -> false
        }
        // We have new price, which is higher, then current - updated order is better, using it
        return if (isCurrentMakePriceLesser) updated else current
    }
}
