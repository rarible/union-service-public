package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.OrderSourceSelectService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class OrderController(
    private val orderSourceSelector: OrderSourceSelectService
) : OrderControllerApi {

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getOrdersAll(blockchains, continuation, size, sort, status)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getOrderBidsByItem(
            itemId,
            platform,
            maker,
            origin,
            status,
            start,
            end,
            continuation,
            size
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getOrderBidsByMaker(
            blockchains,
            platform,
            maker,
            origin,
            status,
            start,
            end,
            continuation,
            size
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderById(id: String): ResponseEntity<OrderDto> {
        val result = orderSourceSelector.getOrderById(id)
        return ResponseEntity.ok(result)
    }

    // TODO UNION add tests
    override suspend fun getOrdersByIds(orderIdsDto: OrderIdsDto): ResponseEntity<OrdersDto> {
        val orders = orderSourceSelector.getByIds(orderIdsDto)
        val result = OrdersDto(orders = orders)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getSellOrders(blockchains, platform, origin, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val result =
            orderSourceSelector.getSellOrdersByItem(itemId, platform, maker, origin, status, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersDto> {
        val result =
            orderSourceSelector.getSellOrdersByMaker(maker, blockchains, platform, origin, continuation, size, status)
        return ResponseEntity.ok(result)
    }
}
