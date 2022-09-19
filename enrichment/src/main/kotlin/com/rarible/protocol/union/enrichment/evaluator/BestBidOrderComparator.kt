package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder

object BestBidOrderComparator : BestOrderComparator {

    override val name: String = "BestBidOrder"

    override fun compare(
        current: ShortOrder,
        updated: ShortOrder
    ): ShortOrder {
        val currentTakePrice = current.takePrice
        val updatedTakePrice = updated.takePrice

        val isCurrentTakePriceLesser = when {
            currentTakePrice == null -> true
            updatedTakePrice != null -> {
                val compared = currentTakePrice.compareTo(updatedTakePrice)
                compared < 0 || compared == 0 &&
                    BestSellOrderComparator.isPreferred(updated) && updated.platform != current.platform
            }
            else -> false
        }
        // We have new price, which is higher, then current - updated order is better, using it
        return if (isCurrentTakePriceLesser) updated else current
    }
}
