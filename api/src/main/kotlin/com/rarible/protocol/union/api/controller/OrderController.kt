package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.service.OrderApiService
import com.rarible.protocol.union.core.continuation.OrderContinuation
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
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
            val (blockchain, shortOrigin) = IdParser.parse(origin)
            val result = router.getService(blockchain).getOrdersAll(platform, shortOrigin, continuation, safeSize)
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
        val (blockchain, shortContract) = IdParser.parse(contract)
        val (makerBlockchain, shortMaker) = safePair(maker, blockchain)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        if (!ensureSameBlockchain(blockchain, makerBlockchain, originBlockchain)) {
            return ResponseEntity.ok(empty)
        }

        val result = orderApiService.getOrderBidsByItem(
            blockchain,
            platform,
            shortContract,
            tokenId,
            shortMaker,
            shortOrigin,
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
        val (blockchain, shortMaker) = IdParser.parse(maker)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        if (!ensureSameBlockchain(blockchain, originBlockchain)) {
            return ResponseEntity.ok(empty)
        }

        val result = router.getService(blockchain)
            .getOrderBidsByMaker(platform, shortMaker, shortOrigin, status, start, end, continuation, safeSize)

        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderById(id: String): ResponseEntity<OrderDto> {
        val (blockchain, shortOrderId) = IdParser.parse(id)
        val result = router.getService(blockchain).getOrderById(shortOrderId)
        return ResponseEntity.ok(result)
    }

    // TODO add tests
    override suspend fun getOrdersByIds(orderIdsDto: OrderIdsDto): ResponseEntity<OrdersDto> {
        val orderIds = orderIdsDto.ids
            .map { IdParser.parse(it) }
            .map { OrderIdDto(blockchain = it.first, value = it.second) }

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
            val (blockchain, shortOrigin) = IdParser.parse(origin)
            val result = router.getService(blockchain).getSellOrders(platform, shortOrigin, continuation, safeSize)
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
        val (blockchain, shortCollection) = IdParser.parse(collection)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        if (!ensureSameBlockchain(blockchain, originBlockchain)) {
            return ResponseEntity.ok(empty)
        }

        val result = router.getService(blockchain)
            .getSellOrdersByCollection(platform, shortCollection, shortOrigin, continuation, safeSize)

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
        val (blockchain, shortContract) = IdParser.parse(contract)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        val (makerBlockchain, shortMaker) = safePair(maker, blockchain)
        if (!ensureSameBlockchain(blockchain, originBlockchain, makerBlockchain)) {
            return ResponseEntity.ok(empty)
        }

        val result = orderApiService.getSellOrdersByItem(
            blockchain, platform, shortContract, tokenId, shortMaker, shortOrigin, status, continuation, safeSize
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
        val (blockchain, shortMaker) = IdParser.parse(maker)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        if (!ensureSameBlockchain(blockchain, originBlockchain)) {
            return ResponseEntity.ok(empty)
        }

        val result = router.getService(blockchain)
            .getSellOrdersByMaker(platform, shortMaker, shortOrigin, continuation, safeSize)

        return ResponseEntity.ok(toDto(result))
    }

    private fun toDto(slice: Slice<OrderDto>): OrdersDto {
        return OrdersDto(
            continuation = slice.continuation,
            orders = slice.entities
        )
    }

    private fun safePair(id: String?, defaultBlockchain: BlockchainDto): Pair<BlockchainDto, String?> {
        return if (id == null) Pair(defaultBlockchain, null) else IdParser.parse(id)
    }

    private fun ensureSameBlockchain(vararg blockchains: BlockchainDto): Boolean {
        val set = blockchains.toSet()
        return set.size == 1
    }
}
