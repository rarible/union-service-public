package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder

object OrderPoolEvaluator {

    fun updatePoolOrderSet(item: ShortItem, order: UnionOrder, action: PoolItemAction): ShortItem {
        val updatedOrder = ShortPoolOrder(order.sellCurrencyId(), ShortOrderConverter.convert(order))
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

    fun needUpdateOrder(item: ShortItem, order: UnionOrder, action: PoolItemAction): Boolean {
        val shortOrder = ShortPoolOrder(order.sellCurrencyId(), ShortOrderConverter.convert(order))
        val poolSellOrders = item.poolSellOrders
        return when (action) {
            PoolItemAction.INCLUDED, PoolItemAction.EXCLUDED -> true
            PoolItemAction.UPDATED -> poolSellOrders.any { match(shortOrder, it) }
        }
    }

    private fun match(order: ShortPoolOrder, other: ShortPoolOrder): Boolean {
        return order.currency == other.currency && order.order.id == other.order.id
    }
}