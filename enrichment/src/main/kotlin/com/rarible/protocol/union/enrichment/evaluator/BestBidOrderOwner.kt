package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder

interface BestBidOrderOwner<T> {

    val bestBidOrders: Map<String, ShortOrder>
    val bestBidOrder: ShortOrder?

    fun withBestBidOrders(orders: Map<String, ShortOrder>): T
    fun withBestBidOrder(order: ShortOrder?): T

}