package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OriginOrdersDto
import com.rarible.protocol.union.enrichment.model.OriginOrders

object OriginOrdersConverter {

    fun convert(
        originOrders: Set<OriginOrders>,
        orders: Map<OrderIdDto, OrderDto>
    ): List<OriginOrdersDto> {
        return originOrders.map { convert(it, orders) }
            .filter { it.bestBidOrder != null || it.bestSellOrder == null }
    }

    fun convert(
        source: OriginOrders,
        orders: Map<OrderIdDto, OrderDto>
    ): OriginOrdersDto {
        return OriginOrdersDto(
            origin = source.origin,
            bestSellOrder = source.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = source.bestBidOrder?.let { orders[it.dtoId] },
        )
    }
}