package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.dto.OrderStatusDto

object OrderStatusConverter {

    fun convert(source: OrderStatusDto): com.rarible.protocol.dto.OrderStatusDto {
        return when (source) {
            OrderStatusDto.ACTIVE -> com.rarible.protocol.dto.OrderStatusDto.ACTIVE
            OrderStatusDto.FILLED -> com.rarible.protocol.dto.OrderStatusDto.FILLED
            OrderStatusDto.HISTORICAL -> com.rarible.protocol.dto.OrderStatusDto.HISTORICAL
            OrderStatusDto.INACTIVE -> com.rarible.protocol.dto.OrderStatusDto.INACTIVE
            OrderStatusDto.CANCELLED -> com.rarible.protocol.dto.OrderStatusDto.CANCELLED
        }
    }

    fun convert(source: List<OrderStatusDto>?): List<com.rarible.protocol.dto.OrderStatusDto>? {
        return source?.map { convert(it) }
    }

}
