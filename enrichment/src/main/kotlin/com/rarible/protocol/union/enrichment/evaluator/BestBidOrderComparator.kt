package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder
import java.math.BigDecimal

abstract class BaseBestBidOrderComparator : BestOrderComparator {
     protected fun compare(
         current: ShortOrder,
         updated: ShortOrder,
         priceExtractor: (ShortOrder) -> BigDecimal?
     ): ShortOrder {
        val currentTakePrice = priceExtractor(current)
        val updatedTakePrice = priceExtractor(updated)

        val isCurrentTakePriceLesser = when {
            currentTakePrice == null -> true
            updatedTakePrice != null -> currentTakePrice <= updatedTakePrice
            else -> false
        }
        // We have new price, which is higher, then current - updated order is better, using it
        return if (isCurrentTakePriceLesser) updated else current
    }
}

object BestBidOrderComparator : BaseBestBidOrderComparator() {
    override val name: String = "BestBidOrder"

    override fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder {
        return compare(current, updated) { shortOrder -> shortOrder.takePrice }
    }
}

object BestUsdBidOrderComparator : BaseBestBidOrderComparator() {
    override val name: String = "BestUsdBidOrder"

    override fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder {
        return compare(current, updated) { shortOrder -> shortOrder.takePriceUsd }
    }
}
