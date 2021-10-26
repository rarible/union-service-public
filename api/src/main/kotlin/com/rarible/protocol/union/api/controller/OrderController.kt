package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.OrderApiService
import com.rarible.protocol.union.core.continuation.OrderContinuation
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.IdParser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderApiService: OrderApiService,
    private val router: BlockchainRouter<OrderService>
) : OrderControllerApi {

    private val empty = OrdersDto(null, emptyList())

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        if (origin != null) {
            val originAddress = IdParser.parseAddress(origin)
            val result = router.getService(originAddress.blockchain)
                .getOrdersAll(platform, originAddress.value, continuation, safeSize)
            return ResponseEntity.ok(toDto(result))
        }

        val blockchainPages = router.executeForAll(blockchains) {
            it.getOrdersAll(platform, null, continuation, safeSize)
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getSlice(safeSize)

        return ResponseEntity.ok(toDto(combinedSlice))
    }

    override suspend fun getOrderBidsByItem(
        contract: String,
        tokenId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val contractAddress = IdParser.parseAddress(contract)
        val makerAddress = safeAddress(maker)
        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(contractAddress, makerAddress, originAddress)) {
            return ResponseEntity.ok(empty)
        }

        val result = orderApiService.getOrderBidsByItem(
            contractAddress.blockchain,
            platform,
            contractAddress.value,
            tokenId,
            makerAddress?.value,
            originAddress?.value,
            status,
            start,
            end,
            continuation,
            safeSize
        )

        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        platform: PlatformDto?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val makerAddress = IdParser.parseAddress(maker)
        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(makerAddress, originAddress)) {
            return ResponseEntity.ok(empty)
        }

        val result = router.getService(makerAddress.blockchain)
            .getOrderBidsByMaker(
                platform,
                makerAddress.value,
                originAddress?.value,
                status,
                start,
                end,
                continuation,
                safeSize
            )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderById(id: String): ResponseEntity<OrderDto> {
        val orderId = IdParser.parseOrderId(id)
        val result = router.getService(orderId.blockchain).getOrderById(orderId.value)
        return ResponseEntity.ok(result)
    }

    // TODO UNION add tests
    override suspend fun getOrdersByIds(orderIdsDto: OrderIdsDto): ResponseEntity<OrdersDto> {
        val orderIds = orderIdsDto.ids
            .map { IdParser.parseOrderId(it) }

        val orders = orderApiService.getByIds(orderIds)
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
        val safeSize = PageSize.ORDER.limit(size)
        if (origin != null) {
            val originAddress = IdParser.parseAddress(origin)
            val result = router.getService(originAddress.blockchain)
                .getSellOrders(platform, originAddress.value, continuation, safeSize)
            return ResponseEntity.ok(toDto(result))
        }

        val blockchainPages = router.executeForAll(blockchains) {
            it.getSellOrders(platform, null, continuation, safeSize)
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getSlice(safeSize)


        return ResponseEntity.ok(toDto(combinedSlice))
    }

    override suspend fun getSellOrdersByCollection(
        collection: String,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val collectionAddress = IdParser.parseAddress(collection)
        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(collectionAddress, originAddress)) {
            return ResponseEntity.ok(empty)
        }

        val result = router.getService(collectionAddress.blockchain)
            .getSellOrdersByCollection(platform, collectionAddress.value, originAddress?.value, continuation, safeSize)

        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val contractAddress = IdParser.parseAddress(contract)
        val originAddress = safeAddress(origin)
        val makerAddress = safeAddress(maker)
        if (!ensureSameBlockchain(contractAddress, originAddress, makerAddress)) {
            return ResponseEntity.ok(empty)
        }

        val result = orderApiService.getSellOrdersByItem(
            contractAddress.blockchain,
            platform,
            contractAddress.value,
            tokenId,
            makerAddress?.value,
            originAddress?.value,
            status,
            continuation,
            safeSize
        )

        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val makerAddress = IdParser.parseAddress(maker)
        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(makerAddress, originAddress)) {
            return ResponseEntity.ok(empty)
        }

        val result = router.getService(makerAddress.blockchain)
            .getSellOrdersByMaker(platform, makerAddress.value, originAddress?.value, continuation, safeSize)

        return ResponseEntity.ok(toDto(result))
    }

    private fun toDto(slice: Slice<OrderDto>): OrdersDto {
        return OrdersDto(
            continuation = slice.continuation,
            orders = slice.entities
        )
    }

    private fun safeAddress(id: String?): UnionAddress? {
        return if (id == null) null else IdParser.parseAddress(id)
    }

    private fun ensureSameBlockchain(vararg blockchains: UnionAddress?): Boolean {
        val set = blockchains.filterNotNull().map { it.blockchain }.toSet()
        return set.size == 1
    }
}
