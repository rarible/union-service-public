package com.rarible.protocol.union.enrichment.service.query.order

import com.rarible.core.common.flatMapAsync
import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.OrderContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.util.BlockchainFilter
import org.springframework.stereotype.Component

@Component
class OrderApiMergeService(
    private val router: BlockchainRouter<OrderService>
) : OrderQueryService {

    companion object {
        private val logger by Logger()
        private val empty = OrdersDto(null, emptyList())
    }

    suspend fun getOrderById(id: String): OrderDto {
        val orderId = IdParser.parseOrderId(id)
        return router.getService(orderId.blockchain).getOrderById(orderId.value)
    }

    suspend fun getByIds(ids: List<OrderIdDto>): List<OrderDto> {
        logger.info("Getting orders by IDs: [{}]", ids.map { "${it.blockchain}:${it.value}" })
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })

        return groupedIds.flatMapAsync { (blockchain, ids) ->
            router.getService(blockchain).getOrdersByIds(ids)
        }
    }

    suspend fun getByIds(orderIdsDto: OrderIdsDto): List<OrderDto> {
        val ids = orderIdsDto.ids
            .map { IdParser.parseOrderId(it) }

        return getByIds(ids)
    }

    override suspend fun getSellOrdersByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val fullItemId = IdParser.parseItemId(itemId)
        val blockchain = fullItemId.blockchain

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
            return empty
        }

        val currencyAssetTypes = router.getService(blockchain)
            .getSellCurrencies(fullItemId.value)
            .map { it.ext.currencyAddress() }

        if (currencyAssetTypes.isEmpty()) {
            return empty
        }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyAssetTypes
        ) { currency, currencyContinuation ->
            router.getService(blockchain).getSellOrdersByItem(
                platform,
                fullItemId.value,
                makerAddress?.value,
                originAddress?.value,
                status,
                currency,
                currencyContinuation,
                safeSize
            )
        }

        // Here we're sorting orders using price evaluated in USD (DESC)
        val slice = ArgPaging(
            OrderContinuation.BySellPriceUsdAndIdAsc,
            currencySlices
        ).getSlice(safeSize)
        return toDto(slice)
    }

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices: List<ArgSlice<OrderDto>> =
            getOrdersByBlockchains(continuation, evaluatedBlockchains) { blockchain, cont ->
                val blockDto = BlockchainDto.valueOf(blockchain)
                router.getService(blockDto).getOrdersAll(cont, safeSize, sort, statuses)
            }
        val slice = ArgPaging(continuationFactory(sort), slices).getSlice(safeSize)
        logger.info(
            "Response for getOrdersAll" +
                    "(blockchains={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            blockchains, continuation, size, slice.entities.size, slice.continuation
        )
        return toDto(slice)
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val result = router.getService(blockchain).getAllSync(continuation, safeSize, sort)
        logger.info(
            "Response for getAllSync" +
                    "(blockchains={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            blockchain, continuation, size, result.entities.size, result.continuation
        )
        return toDto(result)
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
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val fullItemId = IdParser.parseItemId(itemId)

        val makerAddresses = if (maker.isNullOrEmpty()) null else maker.map { IdParser.parseAddress(it) }
        val makerBlockchainGroups = makerAddresses?.let { list -> list.map { it.blockchainGroup } } ?: emptyList()
        val originAddress = safeAddress(origin)
        val blockchain = fullItemId.blockchain
        val makers = makerAddresses?.map { it.value }
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
                fullItemId.fullId(), origin, makers
            )
            return empty
        }

        val currencyContracts = router.getService(blockchain)
            .getBidCurrencies(fullItemId.value)
            .map { it.ext.currencyAddress() }

        if (currencyContracts.isEmpty()) {
            return empty
        }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyContracts
        ) { currency, currencyContinuation ->
            router.getService(blockchain).getOrderBidsByItem(
                platform, fullItemId.value, makers, originAddress?.value, status, start, end, currency, currencyContinuation, safeSize
            )
        }

        // Here we're sorting orders using price evaluated in USD (DESC)
        val slice = ArgPaging(
            OrderContinuation.ByBidPriceUsdAndIdDesc,
            currencySlices
        ).getSlice(safeSize)

        return toDto(slice)
    }

    override suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val makerAddresses = maker.map { IdParser.parseAddress(it) }
        val makerBlockchainGroups = makerAddresses.map { it.blockchainGroup }.toSet()
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)
        if (originAddress != null && !ensureSameBlockchain(makerBlockchainGroups + originAddress.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getOrderBidsByMaker: origin={}, maker={}",
                origin, maker
            )
            return empty
        }

        val blockchainSlices = router.executeForAll(filter.exclude(makerBlockchainGroups)) {
            it.getOrderBidsByMaker(
                platform,
                makerAddresses.filter { address -> address.blockchainGroup.subchains().contains(it.blockchain) }
                    .map { address -> address.value },
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

        return toDto(combinedSlice)
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
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

        return toDto(combinedSlice)
    }

    override suspend fun getSellOrdersByMaker(
        maker: List<String>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val makerAddresses = maker.map { IdParser.parseAddress(it) }
        val makerBlockchainGroups = makerAddresses.map { it.blockchainGroup }.toSet()
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)

        if (originAddress != null && !ensureSameBlockchain(makerBlockchainGroups + originAddress.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getSellOrdersByMaker: origin={}, maker={}",
                origin, maker
            )
            return empty
        }

        val blockchainSlices = router.executeForAll(filter.exclude(makerBlockchainGroups)) {
            it.getSellOrdersByMaker(
                platform,
                makerAddresses.filter { address -> address.blockchainGroup.subchains().contains(it.blockchain) }
                    .map { address -> address.value },
                originAddress?.value,
                status,
                continuation,
                safeSize
            )
        }

        val combinedSlice = Paging(
            OrderContinuation.ByLastUpdatedAndIdDesc,
            blockchainSlices.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getSellOrdersByMaker" +
                    "(maker={}, platform={}, maker={}, origin={}, status={}, continuation={}, size={}): " +
                    "Slice(size={}, continuation={})",
            maker,
            platform,
            maker,
            origin,
            status,
            continuation,
            size,
            combinedSlice.entities.size,
            combinedSlice.continuation
        )

        return toDto(combinedSlice)
    }

    private suspend fun getMultiCurrencyOrdersForItem(
        continuation: String?,
        currencyAssetTypes: List<String>,
        clientCall: suspend (currency: String, continuation: String?) -> Slice<OrderDto>
    ): List<ArgSlice<OrderDto>> {

        val currentContinuation = CombinedContinuation.parse(continuation)

        return currencyAssetTypes.mapAsync { currency ->
            val currencyContinuation = currentContinuation.continuations[currency]
            // For completed currencies we do not request orders
            if (currencyContinuation == ArgSlice.COMPLETED) {
                ArgSlice(currency, currencyContinuation, Slice(null, emptyList()))
            } else {
                ArgSlice(currency, currencyContinuation, clientCall(currency, currencyContinuation))
            }
        }

    }

    private suspend fun getOrdersByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Slice<OrderDto>
    ): List<ArgSlice<OrderDto>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return blockchains.mapAsync { blockchain ->
            val blockchainContinuation = currentContinuation.continuations[blockchain]
            // For completed blockchain we do not request orders
            if (blockchainContinuation == ArgSlice.COMPLETED) {
                ArgSlice(blockchain, blockchainContinuation, Slice(null, emptyList()))
            } else {
                ArgSlice(blockchain, blockchainContinuation, clientCall(blockchain, blockchainContinuation))
            }
        }

    }

    private fun continuationFactory(sort: OrderSortDto?) = when (sort) {
        OrderSortDto.LAST_UPDATE_ASC -> OrderContinuation.ByLastUpdatedAndIdAsc
        OrderSortDto.LAST_UPDATE_DESC, null -> OrderContinuation.ByLastUpdatedAndIdDesc
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

    private fun toDto(slice: Slice<OrderDto>): OrdersDto {
        return OrdersDto(
            continuation = slice.continuation,
            orders = slice.entities
        )
    }
}
