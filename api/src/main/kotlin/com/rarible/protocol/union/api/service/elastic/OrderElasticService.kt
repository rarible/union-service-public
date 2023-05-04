package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.flatMapAsync
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.elastic.EsAllOrderFilter
import com.rarible.protocol.union.core.model.elastic.EsOrder
import com.rarible.protocol.union.core.model.elastic.EsOrderBidOrdersByItem
import com.rarible.protocol.union.core.model.elastic.EsOrderFilter
import com.rarible.protocol.union.core.model.elastic.EsOrderSellOrders
import com.rarible.protocol.union.core.model.elastic.EsOrderSellOrdersByItem
import com.rarible.protocol.union.core.model.elastic.EsOrderSort
import com.rarible.protocol.union.core.model.elastic.EsOrdersByMakers
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderElasticService(
    private val router: BlockchainRouter<OrderService>,
    private val esOrderRepository: EsOrderRepository
) : OrderQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?
    ): Slice<UnionOrder> {
        val safeSize = PageSize.ORDER.limit(size)
        val enabledBlockchains = router.getEnabledBlockchains(blockchains)
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getOrdersAll(), blockchains={}", blockchains)
            return Slice.empty()
        }

        val orderFilter = EsAllOrderFilter(
            blockchains = enabledBlockchains,
            cursor = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = EsOrderSort.of(sort) ?: EsOrderSort.LAST_UPDATE_DESC,
            status = statuses,
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): Slice<UnionOrder> {
        throw UnsupportedOperationException("Operation is not supported for Elastic Search")
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
        if (!router.isBlockchainEnabled(itemId.blockchain)) {
            logger.info("Unable to find enabled blockchains for getSellOrdersByItem(), item={}", itemId)
            return Slice.empty()
        }
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderSellOrdersByItem(
            itemId = itemId.fullId(),
            platform = platform,
            maker = maker?.fullId(),
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getOrderBidsByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        makers: List<UnionAddress>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder> {
        if (!router.isBlockchainEnabled(itemId.blockchain)) {
            logger.info("Unable to find enabled blockchains for getOrderBidsByItem(), item={}", itemId)
            return Slice.empty()
        }
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderBidOrdersByItem(
            itemId = itemId.fullId(),
            platform = platform,
            maker = makers?.map { it.fullId() },
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            currencies = currencies,
        )
        return fetchOrders(orderFilter)
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
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info(
                "Unable to find enabled blockchains for getOrderBidsByMaker(), makers={}, blockchains={}",
                makers, blockchains
            )
            return Slice.empty()
        }

        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrdersByMakers(
            blockchains = enabledBlockchains,
            platform = platform,
            maker = makers.map { it.fullId() },
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = EsOrderSort.LAST_UPDATE_DESC,
            type = EsOrder.Type.BID,
            currencies = currencies,
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder> {
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getSellOrders(), blockchains={}", blockchains)
            return Slice.empty()
        }

        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderSellOrders(
            blockchains = enabledBlockchains,
            platform = platform,
            origin = origin,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = EsOrderSort.LAST_UPDATE_DESC
        )
        return fetchOrders(orderFilter)
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
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info(
                "Unable to find enabled blockchains for getSellOrdersByMaker(), makers={} blockchains={}",
                makers, blockchains
            )
            return Slice.empty()
        }

        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrdersByMakers(
            blockchains = enabledBlockchains,
            platform = platform,
            maker = makers.map { it.fullId() },
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = EsOrderSort.LAST_UPDATE_DESC,
            type = EsOrder.Type.SELL,
            currencies = null,
        )
        return fetchOrders(orderFilter)
    }

    suspend fun fetchOrders(orderFilter: EsOrderFilter): Slice<UnionOrder> {
        val esOrders = esOrderRepository.findByFilter(orderFilter)
        val orderIdsByBlockchain = esOrders.groupBy(EsOrder::blockchain, EsOrder::orderId)

        val slices = orderIdsByBlockchain.flatMapAsync { (blockchain, ids) ->
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                val rawIds = ids.map { IdParser.parseOrderId(it).value }
                router.getService(blockchain).getOrdersByIds(rawIds)
            } else emptyList()
        }.associateBy { it.id.fullId() }

        val sortedOrders = esOrders.mapNotNull { slices[it.orderId] }

        if (esOrders.isEmpty()) {
            return Slice.empty()
        }
        val last = esOrders.last()
        return Slice(
            entities = sortedOrders,
            continuation = DateIdContinuation(
                id = last.orderId,
                date = last.lastUpdatedAt
            ).toString()
        )
    }
}
