package com.rarible.protocol.union.enrichment.model

interface OriginOrdersOwner {

    val originOrders: Set<OriginOrders>

    fun getAllOriginBestOrders(): List<ShortOrder> {
        val result = ArrayList<ShortOrder>()
        originOrders.forEach { orders ->
            orders.bestSellOrder?.let { result.add(it) }
            orders.bestBidOrder?.let { result.add(it) }
        }
        return result
    }

    fun getOriginOrdersMap(): Map<String, OriginOrders> {
        return originOrders.associateBy { it.origin }
    }

    fun getAllBestOrders(): List<ShortOrder>
}