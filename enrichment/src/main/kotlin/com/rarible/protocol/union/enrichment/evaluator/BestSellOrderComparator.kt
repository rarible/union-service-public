package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder
import java.math.BigDecimal

abstract class BaseBestSellOrderComparator : BestOrderComparator {
    protected fun compare(
        current: ShortOrder,
        updated: ShortOrder,
        priceExtractor: (ShortOrder) -> BigDecimal?
    ): ShortOrder {
        val currentMakePrice = priceExtractor(current)
        val updatedMakePrice = priceExtractor(updated)

        val isCurrentMakePriceGreater = when {
            currentMakePrice == null -> true
            updatedMakePrice != null -> currentMakePrice >= updatedMakePrice
            else -> false
        }

        // We have new price, which is lower, then current - updated order is better, using it
        return if (isCurrentMakePriceGreater) updated else current
    }
}

object BestSellOrderComparator : BaseBestSellOrderComparator() {
    override val name: String = "BestSellOrder"

    override fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder {
        return compare(current, updated) { shortOrder -> shortOrder.makePrice }
    }
}

object BestUsdSellOrderComparator : BaseBestSellOrderComparator() {
    override val name: String = "BestUsdSellOrder"

    override fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder {
        return compare(current, updated) { shortOrder -> shortOrder.makePriceUsd }
    }
}
