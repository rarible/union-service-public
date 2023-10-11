package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.elastic.OrderElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionAmmTradeInfo
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderFormDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.enrichment.service.query.order.OrderQueryService
import org.springframework.stereotype.Component

@Component
class OrderSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val orderApiService: OrderApiMergeService,
    private val orderElasticService: OrderElasticService,
) {

    suspend fun upsertOrder(form: OrderFormDto): UnionOrder {
        return orderApiService.upsertOrder(form)
    }

    /**
     * Should always route to OrderApiService
     */
    suspend fun getOrderById(id: String): UnionOrder {
        return orderApiService.getOrderById(id)
    }

    suspend fun getValidatedOrderById(id: String): UnionOrder {
        return orderApiService.getValidatedOrderById(id)
    }

    /**
     * Should always route to OrderApiService
     */
    suspend fun getByIds(orderIdsDto: OrderIdsDto): List<UnionOrder> {
        return orderApiService.getByIds(orderIdsDto)
    }

    suspend fun getAmmOrderTradeInfo(
        id: OrderIdDto,
        itemCount: Int
    ): UnionAmmTradeInfo {
        return orderApiService.getAmmOrderTradeInfo(id, itemCount)
    }

    suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
        searchEngine: SearchEngineDto?
    ): Slice<UnionOrder> {
        return getQuerySource(searchEngine).getOrdersAll(blockchains, continuation, size, sort, status)
    }

    suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): Slice<UnionOrder> {
        return orderApiService.getAllSync(blockchain, continuation, size, sort)
    }

    suspend fun getSellOrdersByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        maker: UnionAddress?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): Slice<UnionOrder> {
        return getQuerySource(searchEngine).getSellOrdersByItem(
            itemId, platform, maker, origin, status, continuation, size
        )
    }

    suspend fun getOrderBidsByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        maker: List<UnionAddress>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): Slice<UnionOrder> {
        return getQuerySource(searchEngine).getOrderBidsByItem(
            itemId,
            platform,
            maker,
            origin,
            status,
            currencies,
            start,
            end,
            continuation,
            size
        )
    }

    suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        maker: List<UnionAddress>,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): Slice<UnionOrder> {
        return getQuerySource(searchEngine).getOrderBidsByMaker(
            blockchains,
            platform,
            maker,
            origin,
            status,
            currencies,
            start,
            end,
            continuation,
            size
        )
    }

    suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): Slice<UnionOrder> {
        return getQuerySource(searchEngine).getSellOrders(blockchains, platform, origin, continuation, size)
    }

    suspend fun getSellOrdersByMaker(
        maker: List<UnionAddress>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        searchEngine: SearchEngineDto?
    ): Slice<UnionOrder> {
        return getQuerySource(searchEngine).getSellOrdersByMaker(
            maker, blockchains, platform, origin, continuation, size, status
        )
    }

    private fun getQuerySource(searchEngine: SearchEngineDto?): OrderQueryService {
        if (searchEngine != null) {
            return when (searchEngine) {
                SearchEngineDto.V1 -> orderElasticService
                SearchEngineDto.LEGACY -> orderApiService
            }
        }

        return when (featureFlagsProperties.enableOrderQueriesToElasticSearch) {
            true -> orderElasticService
            else -> orderApiService
        }
    }
}
