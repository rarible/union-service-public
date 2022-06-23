package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.EsAllOrderFilter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderQueryService
import org.springframework.stereotype.Service

@Service
class OrderElasticService(
    private val router: BlockchainRouter<OrderService>,
    private val esOrderRepository: EsOrderRepository
) : OrderQueryService {

    companion object {
        private val logger by Logger()
    }

    @ExperimentalStdlibApi
    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)

        val orderFilter = EsAllOrderFilter(
            blockchains = evaluatedBlockchains,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = sort ?: OrderSortDto.LAST_UPDATE_DESC,
            status = status,
        )
        val esOrders = esOrderRepository.findByFilter(orderFilter)
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()
        esOrders.forEach { item ->
            mapping
                .computeIfAbsent(item.blockchain) { ArrayList(esOrders.size) }
                .add(item.orderId)
        }

        val slices: List<OrderDto> = mapping.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value

            router.getService(blockchain).getOrdersByIds(ids)
        }.flatten()

        val last = esOrders.last()
        return OrdersDto(
            orders = slices, continuation = DateIdContinuation(
                id = last.orderId,
                date = last.lastUpdatedAt
            ).toString()
        )
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
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        TODO("Not yet implemented")
    }
}
