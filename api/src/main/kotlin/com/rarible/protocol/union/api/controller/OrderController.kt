package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.ApiProperties
import com.rarible.protocol.union.api.service.select.OrderSourceSelectService
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.util.checkNullIds
import com.rarible.protocol.union.dto.AmmTradeInfoDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderFeesDto
import com.rarible.protocol.union.dto.OrderFormDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.CurrencyIdParser
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class OrderController(
    private val orderSourceSelector: OrderSourceSelectService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val apiProperties: ApiProperties
) : OrderControllerApi {

    override suspend fun upsertOrder(orderFormDto: OrderFormDto): ResponseEntity<OrderDto> {
        val result = orderSourceSelector.upsertOrder(orderFormDto)
        return ResponseEntity.ok(enrichmentOrderService.enrich(result))
    }

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getOrdersAll(
            blockchains, continuation, size, sort, status, searchEngine
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getAllSync(blockchain, continuation, size, sort)
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getAmmOrderTradeInfo(
        id: String,
        itemCount: Int
    ): ResponseEntity<AmmTradeInfoDto> {
        val result = orderSourceSelector.getAmmOrderTradeInfo(IdParser.parseOrderId(id), itemCount)
        return ResponseEntity.ok(OrderDtoConverter.convert(result))
    }

    override suspend fun getOrderBidsByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<String>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<OrdersDto> {
        val makers = maker?.map(IdParser::parseAddress)
        val result = orderSourceSelector.getOrderBidsByItem(
            IdParser.parseItemId(itemId),
            platform,
            makers,
            origin,
            status,
            currencies?.map { CurrencyIdParser.parse(it) },
            start,
            end,
            continuation,
            size,
            searchEngine
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderBidsByMaker(
        maker: List<String>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<String>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<OrdersDto> {
        val makers = maker.map(IdParser::parseAddress)
        val result = orderSourceSelector.getOrderBidsByMaker(
            blockchains,
            platform,
            makers,
            origin,
            status,
            currencies?.map { CurrencyIdParser.parse(it) },
            start,
            end,
            continuation,
            size,
            searchEngine
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getOrderById(id: String): ResponseEntity<OrderDto> {
        val result = orderSourceSelector.getOrderById(id)
        return ResponseEntity.ok(enrichmentOrderService.enrich(result))
    }

    override suspend fun getOrderFees(blockchain: BlockchainDto?): ResponseEntity<OrderFeesDto> {
        if (blockchain == null) {
            throw UnionValidationException("Param 'blockchain' is required")
        } else {
            val blockchainFees = apiProperties.orderSettings.fees[blockchain] ?: emptyMap()
            val fees = FEE_TYPE.associateWith { blockchainFees[it] ?: 0 }
            return ResponseEntity.ok(OrderFeesDto(fees))
        }
    }

    override suspend fun getValidatedOrderById(id: String): ResponseEntity<OrderDto> {
        val result = orderSourceSelector.getValidatedOrderById(id)
        return ResponseEntity.ok(enrichmentOrderService.enrich(result))
    }

    // TODO UNION add tests
    override suspend fun getOrdersByIds(orderIdsDto: OrderIdsDto): ResponseEntity<OrdersDto> {
        checkNullIds(orderIdsDto.ids) // It's possible to send request like {"ids": [null]}
        val orders = orderSourceSelector.getByIds(orderIdsDto)
        val result = OrdersDto(orders = enrichmentOrderService.enrich(orders))
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<OrdersDto> {
        val result = orderSourceSelector.getSellOrders(
            blockchains, platform, origin, continuation, size, searchEngine
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getSellOrdersByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<OrdersDto> {
        val makerAddress = maker?.let(IdParser::parseAddress)
        val result = orderSourceSelector.getSellOrdersByItem(
            IdParser.parseItemId(itemId), platform, makerAddress, origin, status, continuation, size, searchEngine
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getSellOrdersByMaker(
        maker: List<String>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<OrdersDto> {
        val makers = maker.map(IdParser::parseAddress)
        val result = orderSourceSelector.getSellOrdersByMaker(
            makers, blockchains, platform, origin, continuation, size, status, searchEngine
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun reportOrderById(id: String): ResponseEntity<Unit> {
        orderSourceSelector.reportOrder(id)
        return ResponseEntity.ok().build()
    }

    private suspend fun toDto(slice: Slice<UnionOrder>): OrdersDto {
        val orders = enrichmentOrderService.enrich(slice.entities)
        return OrdersDto(slice.continuation, orders)
    }

    companion object {
        private val FEE_TYPE = listOf(
            "RARIBLE_V1",
            "RARIBLE_V2",
            "OPEN_SEA_V1",
            "SEAPORT_V1",
            "LOOKSRARE",
            "LOOKSRARE_V2",
            "CRYPTO_PUNK",
            "AMM",
            "X2Y2",
            "AUCTION"
        )
    }
}
