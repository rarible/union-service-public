package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import com.rarible.protocol.union.enrichment.util.sellCurrencyId

object OrderPoolEvaluator {

    fun updatePoolOrderSet(item: ShortItem, order: OrderDto, action: PoolItemAction): ShortItem {
        val updatedOrder = ShortPoolOrder(order.sellCurrencyId, ShortOrderConverter.convert(order))
        val poolSellOrders = item.poolSellOrders
        val currentOrder = poolSellOrders.find { match(updatedOrder, it) }
        return when (action) {
            PoolItemAction.INCLUDED -> {
                val updatedPoolOrders = currentOrder?.let { poolSellOrders - currentOrder } ?: poolSellOrders
                item.copy(poolSellOrders = updatedPoolOrders + updatedOrder)
            }
            PoolItemAction.EXCLUDED -> {
                val updatedPoolOrders = currentOrder?.let { poolSellOrders - currentOrder } ?: poolSellOrders
                item.copy(poolSellOrders = updatedPoolOrders)
            }
            PoolItemAction.UPDATED -> {
                item
            }
        }
    }

    fun hasPoolOrder(item: ShortItem, order: OrderDto): Boolean {
        val shortOrder = ShortPoolOrder(order.sellCurrencyId, ShortOrderConverter.convert(order))
        val poolSellOrders = item.poolSellOrders
        return poolSellOrders.any { match(shortOrder, it) }
    }

    private fun match(order: ShortPoolOrder, other: ShortPoolOrder): Boolean {
        return order.currency == other.currency && order.order.id == other.order.id
    }
}