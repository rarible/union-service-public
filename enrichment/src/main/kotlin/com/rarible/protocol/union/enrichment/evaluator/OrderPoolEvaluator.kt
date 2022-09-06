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
        val currentOrder = poolSellOrders.find {
            updatedOrder.currency == it.currency && updatedOrder.order.id == it.order.id
        }

        return if (action == PoolItemAction.INCLUDED) {
            val updatedPoolOrders = currentOrder?.let { poolSellOrders - currentOrder } ?: poolSellOrders
            item.copy(poolSellOrders = updatedPoolOrders + updatedOrder)
        } else {
            val updatedPoolOrders = currentOrder?.let { poolSellOrders - currentOrder } ?: poolSellOrders
            item.copy(poolSellOrders = updatedPoolOrders)
        }
    }

}