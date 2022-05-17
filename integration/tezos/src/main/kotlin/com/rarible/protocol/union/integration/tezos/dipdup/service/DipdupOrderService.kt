package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import java.math.BigInteger

interface DipdupOrderService {

    fun enabled() = false

    suspend fun getOrderById(id: String): OrderDto {
        TODO("Not implemented")
    }

    suspend fun getOrderByIds(id: List<String>): List<OrderDto> {
        TODO("Not implemented")
    }

    suspend fun getOrdersAll(
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not implemented")
    }

    suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: BigInteger,
        maker: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not implemented")
    }
}


