package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.flatMapAsync
import com.rarible.protocol.union.core.model.EsAllOrderFilter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOrderBidOrdersByItem
import com.rarible.protocol.union.core.model.EsOrderFilter
import com.rarible.protocol.union.core.model.EsOrderSellOrders
import com.rarible.protocol.union.core.model.EsOrderSellOrdersByItem
import com.rarible.protocol.union.core.model.EsOrderSort
import com.rarible.protocol.union.core.model.EsOrdersByMakers
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderQueryService
import org.springframework.stereotype.Service

@Service
class OrderElasticService(
    private val router: BlockchainRouter<OrderService>,
    private val esOrderRepository: EsOrderRepository
) : OrderQueryService {

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)

        val orderFilter = EsAllOrderFilter(
            blockchains = evaluatedBlockchains,
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
    ): OrdersDto {
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
    ): OrdersDto {
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
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderBidOrdersByItem(
            itemId = itemId.fullId(),
            platform = platform,
            maker = makers?.map { it.fullId() },
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        makers: List<UnionAddress>,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrdersByMakers(
            platform = platform,
            maker = makers.map { it.fullId() },
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = EsOrderSort.LAST_UPDATE_DESC,
            type = EsOrder.Type.BID
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderSellOrders(
            blockchains = blockchains,
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
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrdersByMakers(
            platform = platform,
            maker = makers.map { it.fullId() },
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = EsOrderSort.LAST_UPDATE_DESC,
            type = EsOrder.Type.SELL
        )
        return fetchOrders(orderFilter)
    }

    suspend fun fetchOrders(orderFilter: EsOrderFilter): OrdersDto {
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

        return if (esOrders.isEmpty()) {
            OrdersDto(orders = emptyList(), continuation = null)
        } else {
            val last = esOrders.last()
            OrdersDto(
                orders = sortedOrders,
                continuation = DateIdContinuation(
                    id = last.orderId,
                    date = last.lastUpdatedAt
                ).toString()
            )
        }
    }
}
