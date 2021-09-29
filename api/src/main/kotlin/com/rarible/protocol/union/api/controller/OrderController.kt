package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.core.continuation.Paging
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.service.OrderServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.OrderContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val router: OrderServiceRouter
) : OrderControllerApi {

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
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val (blockchain, shortContract) = IdParser.parse(contract)
        val (makerBlockchain, shortMaker) = safePair(maker, blockchain)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        ensureSameBlockchain(blockchain, makerBlockchain, originBlockchain)

        val result = router.getService(blockchain)
            .getOrderBidsByItem(platform, shortContract, tokenId, shortMaker, shortOrigin, continuation, safeSize)
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val (blockchain, shortMaker) = IdParser.parse(maker)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        ensureSameBlockchain(blockchain, originBlockchain)

        val result = router.getService(blockchain)
            .getOrderBidsByMaker(platform, shortMaker, shortOrigin, continuation, safeSize)

        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderById(id: String): ResponseEntity<OrderDto> {
        val (blockchain, shortOrderId) = IdParser.parse(id)
        val result = router.getService(blockchain).getOrderById(shortOrderId)
        return ResponseEntity.ok(result)
    }

    // TODO add tests
    @ExperimentalCoroutinesApi
    override fun getOrdersByIds(orderIdsDto: OrderIdsDto): ResponseEntity<Flow<OrderDto>> {
        val groupedIds = orderIdsDto
            .ids.map { IdParser.parse(it) }
            .groupBy({ it.first }, { it.second })

        val combinedOrders = groupedIds
            .map { router.getService(it.key).getOrdersByIds(it.value) }
            .merge()

        return ResponseEntity.ok(combinedOrders)
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
        ensureSameBlockchain(blockchain, originBlockchain)

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
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val (blockchain, shortContract) = IdParser.parse(contract)
        val (originBlockchain, shortOrigin) = safePair(origin, blockchain)
        val (makerBlockchain, shortMaker) = safePair(maker, blockchain)
        ensureSameBlockchain(blockchain, originBlockchain, makerBlockchain)

        val result = router.getService(blockchain)
            .getSellOrdersByItem(platform, shortContract, tokenId, shortMaker, shortOrigin, continuation, safeSize)

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
        ensureSameBlockchain(blockchain, originBlockchain)

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

    private fun ensureSameBlockchain(vararg blockchains: BlockchainDto) {
        val set = blockchains.toSet()
        if (set.size != 1) {
            throw IllegalArgumentException("All of arguments should belong to same blockchain, but received: $set")
        }
    }
}