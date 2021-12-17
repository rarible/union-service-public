package com.rarible.protocol.union.api.controller

import com.rarible.core.logging.withMdc
import com.rarible.protocol.union.api.service.OrderApiService
import com.rarible.protocol.union.api.service.extractItemId
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.OrderContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
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
        platform: PlatformDto?,
        itemId: String?,
        contract: String?,
        tokenId: String?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val fullItemId = extractItemId(contract, tokenId, itemId)

        val makerAddress = safeAddress(maker)
        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(
                fullItemId.blockchain.group(),
                makerAddress?.blockchainGroup,
                originAddress?.blockchainGroup
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
            platform,
            fullItemId.contract,
            fullItemId.tokenId.toString(),
            makerAddress?.value,
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

    //--------- TODO UNION - this method should be implemented with currencies, like getBidsByItem ---------//
    // For now it is hidden from openapi since nobody using it
    @ExperimentalCoroutinesApi
    @GetMapping(
        value = ["/v0.1/orders/bids/byMaker"],
        produces = ["application/json"]
    )
    suspend fun getOrderBidsByMaker0(
        @RequestParam(value = "maker", required = true) maker: kotlin.String,
        @RequestParam(value = "platform", required = false) platform: PlatformDto?,
        @RequestParam(value = "origin", required = false) origin: kotlin.String?,
        @RequestParam(value = "status", required = false) status: kotlin.collections.List<OrderStatusDto>?,
        @RequestParam(value = "start", required = false) start: kotlin.Long?,
        @RequestParam(value = "end", required = false) end: kotlin.Long?,
        @RequestParam(value = "continuation", required = false) continuation: kotlin.String?,
        @RequestParam(value = "size", required = false) size: kotlin.Int?
    ): ResponseEntity<OrdersDto> {
        return withMdc { getOrderBidsByMaker(maker, platform, origin, status, start, end, continuation, size) }
    }

    suspend fun getOrderBidsByMaker(
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
        if (!ensureSameBlockchain(makerAddress.blockchainGroup, originAddress?.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getOrderBidsByMaker: origin={}, maker={}",
                origin, maker
            )
            return ResponseEntity.ok(empty)
        }

        val blockchainSlices = router.executeForAll(makerAddress.blockchainGroup.subchains()) {
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
            OrderContinuation.ByBidPriceUsdAndIdDesc, // TODO UNION - Should be by price in USD
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
//-----------------------------------------------------------------------//

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
        val evaluatedBlockchains = originAddress?.blockchainGroup?.subchains() ?: blockchains

        val blockchainPages = router.executeForAll(evaluatedBlockchains) {
            it.getSellOrders(platform, originAddress?.value, continuation, safeSize)
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndId,
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
        platform: PlatformDto?,
        itemId: String?,
        contract: String?,
        tokenId: String?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val fullItemId = extractItemId(contract, tokenId, itemId)

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
            platform,
            fullItemId.contract,
            fullItemId.tokenId.toString(),
            makerAddress?.value,
            originAddress?.value,
            status,
            continuation,
            safeSize
        )

        logger.info(
            "Response for getSellOrdersByItem" +
                    "(contract={}, tokenId={}, platform={}, maker={}, origin={}, status={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            contract, tokenId, platform, maker, origin, status, continuation, size,
            result.entities.size, result.continuation
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
        if (!ensureSameBlockchain(makerAddress.blockchainGroup, originAddress?.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getSellOrdersByMaker: origin={}, maker={}",
                origin, maker
            )
            return ResponseEntity.ok(empty)
        }

        val blockchainSlices = router.executeForAll(makerAddress.blockchainGroup.subchains()) {
            it.getSellOrdersByMaker(platform, makerAddress.value, originAddress?.value, continuation, safeSize)
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndId,
            blockchainSlices.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getSellOrdersByMaker" +
                    "(maker={}, platform={}, maker={}, origin={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            maker, platform, maker, origin, continuation, size, combinedSlice.entities.size, combinedSlice.continuation
        )

        return ResponseEntity.ok(toDto(combinedSlice))
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
        val set = blockchains.filterNotNull().toSet()
        return set.size == 1
    }
}
