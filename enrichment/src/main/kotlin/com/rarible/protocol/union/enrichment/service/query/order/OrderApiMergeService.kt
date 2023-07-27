package com.rarible.protocol.union.enrichment.service.query.order

import com.rarible.core.common.flatMapAsync
import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.continuation.UnionOrderContinuation
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.util.BlockchainFilter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderApiMergeService(
    private val router: BlockchainRouter<OrderService>,
    private val enrichmentItemService: EnrichmentItemService
) : OrderQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val onlyActiveOrdersFilter = listOf(OrderStatusDto.ACTIVE)

    suspend fun getOrderById(id: String): UnionOrder {
        val orderId = IdParser.parseOrderId(id)
        return router.getService(orderId.blockchain).getOrderById(orderId.value)
    }

    suspend fun getByIds(ids: List<OrderIdDto>): List<UnionOrder> {
        logger.info(
            "Getting {} orders by IDs: first 100 [{}]",
            ids.size, ids.take(100).map { "${it.blockchain}:${it.value}" }
        )
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })

        return groupedIds.flatMapAsync { (blockchain, ids) ->
            router.getService(blockchain).getOrdersByIds(ids)
        }
    }

    suspend fun getByIds(orderIdsDto: OrderIdsDto): List<UnionOrder> {
        val ids = orderIdsDto.ids
            .map { IdParser.parseOrderId(it) }

        return getByIds(ids)
    }

    override suspend fun getSellOrdersByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        maker: UnionAddress?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val blockchain = itemId.blockchain

        val originAddress = safeAddress(origin)
        if (!ensureSameBlockchain(
                itemId.blockchain.group(),
                originAddress?.blockchainGroup,
                maker?.blockchainGroup
            )
        ) {
            logger.warn(
                "Incompatible blockchain groups specified in getSellOrdersByItem: itemId={}, origin={}, maker={}",
                itemId.fullId(), origin, maker
            )
            return Slice.empty()
        }

        val currencyAssetTypes = when (status) {
            // For ACTIVE orders we don't need to fetch currencies - we can take them from item
            onlyActiveOrdersFilter -> getSellCurrenciesFromItem(itemId)
            else -> router.getService(blockchain).getSellCurrencies(itemId.value, status).map { it.currencyId()!! }
        }

        if (currencyAssetTypes.isEmpty()) {
            return Slice.empty()
        }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyAssetTypes
        ) { currency, currencyContinuation ->
            router.getService(blockchain).getSellOrdersByItem(
                platform,
                itemId.value,
                maker?.value,
                originAddress?.value,
                status,
                currency,
                currencyContinuation,
                safeSize
            )
        }

        // Here we're sorting orders using price evaluated in USD (DESC)
        return ArgPaging(
            UnionOrderContinuation.BySellPriceUsdAndIdAsc,
            currencySlices
        ).getSlice(safeSize)
    }

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices: List<ArgSlice<UnionOrder>> =
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
        return slice
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val result = router.getService(blockchain).getAllSync(continuation, safeSize, sort)
        logger.info(
            "Response for getAllSync" +
                "(blockchains={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            blockchain, continuation, size, result.entities.size, result.continuation
        )
        return result
    }

    override suspend fun getOrderBidsByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        maker: List<UnionAddress>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)

        val makerBlockchainGroups = maker?.let { list -> list.map { it.blockchainGroup } } ?: emptyList()
        val originAddress = safeAddress(origin)
        val blockchain = itemId.blockchain
        if (!ensureSameBlockchain(
                makerBlockchainGroups +
                    listOf(
                            itemId.blockchain.group(),
                            originAddress?.blockchainGroup
                        ),
            )
        ) {
            logger.warn(
                "Incompatible blockchain groups specified in getOrderBidsByItem: itemId={}, origin={}, makers={}",
                itemId.fullId(), origin, maker?.map { it.fullId() }
            )
            return Slice.empty()
        }

        val currencyContracts = when {
            currencies != null -> currencies.map { it.value }
            // We can't use item's currencies for ACTIVE bids - there can be Floor bids
            // status == onlyActiveOrdersFilter -> getBidCurrenciesFromItem(itemId)
            else -> router.getService(blockchain).getBidCurrencies(itemId.value, status).map { it.currencyId()!! }
        }

        if (currencyContracts.isEmpty()) {
            return Slice.empty()
        }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyContracts
        ) { currency, currencyContinuation ->
            router.getService(blockchain).getOrderBidsByItem(
                platform,
                itemId.value,
                maker?.map { it.value },
                originAddress?.value,
                status,
                start,
                end,
                currency,
                currencyContinuation,
                safeSize
            )
        }

        // Here we're sorting orders using price evaluated in USD (DESC)
        return ArgPaging(
            UnionOrderContinuation.ByBidPriceUsdAndIdDesc,
            currencySlices
        ).getSlice(safeSize)
    }

    override suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        makers: List<UnionAddress>,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val makerBlockchainGroups = makers.map { it.blockchainGroup }.toSet()
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)
        if (originAddress != null && !ensureSameBlockchain(makerBlockchainGroups + originAddress.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getOrderBidsByMaker: origin={}, maker={}",
                origin, makers.map { it.fullId() }
            )
            return Slice.empty()
        }

        val blockchainSlices = router.executeForAll(filter.exclude(makerBlockchainGroups)) {
            it.getOrderBidsByMaker(
                platform,
                makers.filter { address -> address.blockchainGroup.subchains().contains(it.blockchain) }
                    .map { address -> address.value },
                originAddress?.value,
                status,
                currencies?.map { currency -> currency.value },
                start,
                end,
                continuation,
                safeSize
            )
        }

        val combinedSlice = Paging(
            UnionOrderContinuation.ByLastUpdatedAndIdDesc,
            blockchainSlices.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getOrderBidsByMaker" +
                "(maker={}, platform={}, origin={}, status={}, start={}, end={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            makers.map { it.fullId() }, platform, origin, status, start, end, continuation, size,
            combinedSlice.entities.size, combinedSlice.continuation
        )

        return combinedSlice
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)

        val evaluatedBlockchains = originAddress?.blockchainGroup?.let { filter.exclude(it) } ?: blockchains

        val blockchainPages = router.executeForAll(evaluatedBlockchains) {
            it.getSellOrders(platform, originAddress?.value, continuation, safeSize)
        }

        val combinedSlice = Paging(
            UnionOrderContinuation.ByLastUpdatedAndIdDesc,
            blockchainPages.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getSellOrders(blockchains={}, platform={}, origin={}, continuation={}, size={}):" +
                " Slice(size={}, continuation={}) from blockchain slices {} ",
            evaluatedBlockchains, platform, origin, continuation, size,
            combinedSlice.entities.size, combinedSlice.continuation, blockchainPages.map { it.entities.size }
        )

        return combinedSlice
    }

    override suspend fun getSellOrdersByMaker(
        makers: List<UnionAddress>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val makerBlockchainGroups = makers.map { it.blockchainGroup }.toSet()
        val originAddress = safeAddress(origin)
        val filter = BlockchainFilter(blockchains)

        if (originAddress != null && !ensureSameBlockchain(makerBlockchainGroups + originAddress.blockchainGroup)) {
            logger.warn(
                "Incompatible blockchain groups specified in getSellOrdersByMaker: origin={}, makers={}",
                origin, makers.map { it.fullId() }
            )
            return Slice.empty()
        }

        val blockchainSlices = router.executeForAll(filter.exclude(makerBlockchainGroups)) {
            it.getSellOrdersByMaker(
                platform,
                makers.filter { address -> address.blockchainGroup.subchains().contains(it.blockchain) }
                    .map { address -> address.value },
                originAddress?.value,
                status,
                continuation,
                safeSize
            )
        }

        val combinedSlice = Paging(
            UnionOrderContinuation.ByLastUpdatedAndIdDesc,
            blockchainSlices.flatMap { it.entities }
        ).getSlice(safeSize)

        logger.info(
            "Response for getSellOrdersByMaker" +
                "(maker={}, platform={}, origin={}, status={}, continuation={}, size={}): " +
                "Slice(size={}, continuation={})",
            makers.map { it.fullId() },
            platform,
            origin,
            status,
            continuation,
            size,
            combinedSlice.entities.size,
            combinedSlice.continuation
        )
        return combinedSlice
    }

    private suspend fun getMultiCurrencyOrdersForItem(
        continuation: String?,
        currencyAssetTypes: List<String>,
        clientCall: suspend (currency: String, continuation: String?) -> Slice<UnionOrder>
    ): List<ArgSlice<UnionOrder>> {

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
        clientCall: suspend (blockchain: String, continuation: String?) -> Slice<UnionOrder>
    ): List<ArgSlice<UnionOrder>> {
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
        OrderSortDto.LAST_UPDATE_ASC -> UnionOrderContinuation.ByLastUpdatedAndIdAsc
        OrderSortDto.LAST_UPDATE_DESC, null -> UnionOrderContinuation.ByLastUpdatedAndIdDesc
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

    private suspend fun getSellCurrenciesFromItem(itemId: ItemIdDto): List<String> {
        val item = enrichmentItemService.get(ShortItemId(itemId)) ?: return emptyList()
        return item.bestSellOrders.keys.toList()
    }

    private suspend fun getBidCurrenciesFromItem(itemId: ItemIdDto): List<String> {
        val item = enrichmentItemService.get(ShortItemId(itemId)) ?: return emptyList()
        return item.bestBidOrders.keys.toList()
    }
}
