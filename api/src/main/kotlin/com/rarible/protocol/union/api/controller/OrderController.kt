package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.OrderApiService
import com.rarible.protocol.union.api.util.BlockchainFilter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrderSyncSortDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.OrderContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.dto.parser.IdParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class OrderController(
    private val orderApiService: OrderApiService,
    private val router: BlockchainRouter<OrderService>
) : OrderControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val empty = OrdersDto(null, emptyList())

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val result = orderApiService.getOrdersAll(blockchains, continuation, safeSize, sort, status)
        logger.info(
            "Response for getOrdersAll" +
                "(blockchains={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            blockchains, continuation, size, result.entities.size, result.continuation
        )
        return ResponseEntity.ok(toDto(result))
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
        val safeSize = PageSize.ORDER.limit(size)
        val fullItemId = IdParser.parseItemId(itemId)

        val makerAddresses = if (maker.isNullOrEmpty()) null else maker.map { IdParser.parseAddress(it) }
        val makerBlockchainGroups = makerAddresses?.let { list -> list.map { it.blockchainGroup } } ?: emptyList()
        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(
                makerBlockchainGroups +
                    listOf(
                        fullItemId.blockchain.group(),
                        originAddress?.blockchainGroup
                    ),
            )
        ) {
            logger.warn(
                "Incompatible blockchain groups specified in getOrderBidsByItem: itemId={}, origin={}, maker={}",
                fullItemId.fullId(), origin, maker
            )
            return ResponseEntity.ok(empty)
        }

        val result = orderApiService.getOrderBidsByItem(
            fullItemId.blockchain,
            fullItemId.value,
            platform,
            makerAddresses?.map { it.value },
            originAddress?.value,
            status,
            start,
            end,
            continuation,
            safeSize
        )

        logger.info(
            "Response for getOrderBidsByItem" +
                "(itemId={}, platform={}, maker={}, origin={}, status={}, start={}, end={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            fullItemId.fullId(), platform, maker, origin, status, start, end, continuation, size,
            result.entities.size, result.continuation
        )

        return ResponseEntity.ok(toDto(result))
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
        val safeSize = PageSize.ORDER.limit(size)
        val makerAddress = IdParser.parseAddress(maker)
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)
        if (!ensureSameBlockchain(makerAddress.blockchainGroup, originAddress?.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getOrderBidsByMaker: origin={}, maker={}",
                origin, maker
            )
            return ResponseEntity.ok(empty)
        }

        val blockchainSlices = router.executeForAll(filter.exclude(makerAddress.blockchainGroup)) {
            it.getOrderBidsByMaker(
                platform,
                makerAddress.value,
                originAddress?.value,
                status,
                start,
                end,
                continuation,
                safeSize
            )
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndIdAsc,
            blockchainSlices.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getOrderBidsByMaker" +
                "(maker={}, platform={}, origin={}, status={}, start={}, end={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            maker, platform, origin, status, start, end, continuation, size,
            combinedSlice.entities.size, combinedSlice.continuation
        )

        return ResponseEntity.ok(toDto(combinedSlice))
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
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)

        val evaluatedBlockchains = originAddress?.blockchainGroup?.let { filter.exclude(it) } ?: blockchains

        val blockchainPages = router.executeForAll(evaluatedBlockchains) {
            it.getSellOrders(platform, originAddress?.value, continuation, safeSize)
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndIdDesc,
            blockchainPages.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info("Response for getSellOrders(blockchains={}, platform={}, origin={}, continuation={}, size={}):" +
            " Slice(size={}, continuation={}) from blockchain slices {} ",
            evaluatedBlockchains, platform, origin, continuation, size,
            combinedSlice.entities.size, combinedSlice.continuation, blockchainPages.map { it.entities.size }
        )

        return ResponseEntity.ok(toDto(combinedSlice))
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
        val safeSize = PageSize.ORDER.limit(size)
        val fullItemId = IdParser.parseItemId(itemId)

        val originAddress = safeAddress(origin)
        val makerAddress = safeAddress(maker)
        if (!ensureSameBlockchain(
                fullItemId.blockchain.group(),
                originAddress?.blockchainGroup,
                makerAddress?.blockchainGroup
            )
        ) {
            logger.warn(
                "Incompatible blockchain groups specified in getSellOrdersByItem: itemId={}, origin={}, maker={}",
                fullItemId.fullId(), origin, maker
            )
            return ResponseEntity.ok(empty)
        }

        val result = orderApiService.getSellOrdersByItem(
            fullItemId.blockchain,
            fullItemId.value,
            platform,
            makerAddress?.value,
            originAddress?.value,
            status,
            continuation,
            safeSize
        )

        logger.info(
            "Response for getSellOrdersByItem" +
                "(itemId={}, platform={}, maker={}, origin={}, status={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            itemId, platform, maker, origin, status, continuation, size, result.entities.size, result.continuation
        )

        return ResponseEntity.ok(toDto(result))
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
        val safeSize = PageSize.ORDER.limit(size)
        val makerAddress = IdParser.parseAddress(maker)
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)

        if (!ensureSameBlockchain(makerAddress.blockchainGroup, originAddress?.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getSellOrdersByMaker: origin={}, maker={}",
                origin, maker
            )
            return ResponseEntity.ok(empty)
        }

        val blockchainSlices = router.executeForAll(filter.exclude(makerAddress.blockchainGroup)) {
            it.getSellOrdersByMaker(platform, makerAddress.value, originAddress?.value, status, continuation, safeSize)
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndIdDesc,
            blockchainSlices.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getSellOrdersByMaker" +
                    "(maker={}, platform={}, maker={}, origin={}, status={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            maker, platform, maker, origin, status, continuation, size, combinedSlice.entities.size, combinedSlice.continuation
        )

        return ResponseEntity.ok(toDto(combinedSlice))
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: OrderSyncSortDto?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val result = orderApiService.getAllSync(blockchain, continuation, safeSize, sort)
        logger.info(
            "Response for getAllSync" +
                    "(blockchains={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            blockchain, continuation, size, result.entities.size, result.continuation
        )
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

    private fun ensureSameBlockchain(vararg blockchains: BlockchainGroupDto?): Boolean {
        return ensureSameBlockchain(blockchains.asList())
    }

    private fun ensureSameBlockchain(blockchains: Collection<BlockchainGroupDto?>): Boolean {
        val set = blockchains.filterNotNull().toSet()
        return set.size == 1
    }
}
