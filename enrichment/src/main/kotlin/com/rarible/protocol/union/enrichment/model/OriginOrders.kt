package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner

data class OriginOrders(
    val origin: String,
    override val bestSellOrder: ShortOrder? = null,
    override val bestSellOrders: Map<String, ShortOrder> = emptyMap(),
    override val bestBidOrder: ShortOrder? = null,
    override val bestBidOrders: Map<String, ShortOrder> = emptyMap()
) : BestSellOrderOwner<OriginOrders>, BestBidOrderOwner<OriginOrders> {

    override fun withBestSellOrders(orders: Map<String, ShortOrder>): OriginOrders {
        return this.copy(bestSellOrders = orders)
    }

    override fun withBestSellOrder(order: ShortOrder?): OriginOrders {
        return this.copy(bestSellOrder = order)
    }

    override fun withBestBidOrders(orders: Map<String, ShortOrder>): OriginOrders {
        return this.copy(bestBidOrders = orders)
    }

    override fun withBestBidOrder(order: ShortOrder?): OriginOrders {
        return this.copy(bestBidOrder = order)
    }

    fun isEmpty(): Boolean {
        return bestSellOrder == null && bestBidOrder == null
    }
}