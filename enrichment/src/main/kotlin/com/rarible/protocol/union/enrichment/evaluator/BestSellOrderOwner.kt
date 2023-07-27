package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder

interface BestSellOrderOwner<T> {

    val bestSellOrders: Map<String, ShortOrder>
    val bestSellOrder: ShortOrder?

    fun withBestSellOrders(orders: Map<String, ShortOrder>): T
    fun withBestSellOrder(order: ShortOrder?): T
}
