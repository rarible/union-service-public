package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder

object BestSellOrderComparator : BestOrderComparator {

    override val name: String = "BestSellOrder"

    override fun compare(
        current: ShortOrder,
        updated: ShortOrder
    ): ShortOrder {
        val currentMakePrice = current.makePrice
        val updatedMakePrice = updated.makePrice

        val isCurrentMakePriceGreater = when {
            currentMakePrice == null -> true
            updatedMakePrice != null -> currentMakePrice >= updatedMakePrice
            else -> false
        }

        // We have new price, which is lower, then current - updated order is better, using it
        return if (isCurrentMakePriceGreater) updated else current
    }
}
