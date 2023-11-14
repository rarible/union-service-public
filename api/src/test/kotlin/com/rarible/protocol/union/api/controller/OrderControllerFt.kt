package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.AmmTradeInfoDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.order.api.client.OrderControllerApi.ErrorUpsertOrder
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SudoSwapTradeInfoDto
import com.rarible.protocol.union.dto.UnionApiErrorBadRequestDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.UnionOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAmmPriceInfoDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonAddress
import com.rarible.protocol.union.integration.flow.data.randomFlowAddress
import com.rarible.protocol.union.integration.flow.data.randomFlowV1OrderDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException
import randomEthRaribleV2OrderFormDto
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import scalether.domain.Address
import java.math.BigDecimal

@FlowPreview
@IntegrationTest
class OrderControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ORDER.default
    private val platform: PlatformDto? = null
    private val ethPlatform: com.rarible.protocol.dto.PlatformDto? = null

    @Autowired
    lateinit var orderControllerClient: OrderControllerApi

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Test
    fun `upsert order - ethereum`() = runBlocking<Unit> {
        val order = randomEthSellOrderDto()
        val form = randomEthRaribleV2OrderFormDto()
        val nativeForm = UnionOrderConverter.convert(form)

        ethereumOrderControllerApiMock.mockUpsertOrder(nativeForm, order)

        val unionOrder = orderControllerClient.upsertOrder(form).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(order.hash.prefixed())
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `upsert order error - ethereum`() = runBlocking<Unit> {
        val form = randomEthRaribleV2OrderFormDto()
        val nativeForm = UnionOrderConverter.convert(form)

        val error = ErrorUpsertOrder(WebClientResponseException(400, "", null, null, null))
        error.on400 = EthereumOrderUpdateApiErrorDto(EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE, "Ooops")
        error.data = error.on400

        every { testEthereumOrderApi.upsertOrder(nativeForm) } returns Mono.error(error) // ethereum error

        StepVerifier.create(orderControllerClient.upsertOrder(form))
            .expectErrorSatisfies { e ->
                assertThat(e).isInstanceOf(OrderControllerApi.ErrorUpsertOrder::class.java) // union error
                assertThat((e as OrderControllerApi.ErrorUpsertOrder).on500).isNull()
                assertThat(e.on400.code).isEqualTo(UnionApiErrorBadRequestDto.Code.BAD_REQUEST)
                assertThat(e.on400.message).isEqualTo("Ooops")
            }
            .verify()
    }

    @Test
    fun `get order by id - ethereum`() = runBlocking<Unit> {
        val order = randomEthSellOrderDto()
        val orderId = EthConverter.convert(order.hash)
        val orderIdFull = OrderIdDto(BlockchainDto.ETHEREUM, order.hash.prefixed()).fullId()

        ethereumOrderControllerApiMock.mockGetById(order)

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(orderId)
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get validated order by id - ethereum`() = runBlocking<Unit> {
        val order = randomEthSellOrderDto()
        val orderId = EthConverter.convert(order.hash)
        val orderIdFull = OrderIdDto(BlockchainDto.ETHEREUM, order.hash.prefixed()).fullId()

        ethereumOrderControllerApiMock.mockGetById(order)

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(orderId)
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get order by id - tezos`() = runBlocking<Unit> {
        val order = randomTezosOrderDto()
        val orderIdFull = OrderIdDto(BlockchainDto.TEZOS, order.id).fullId()

        tezosOrderControllerApiMock.mockGetById(order)

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(order.id)
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.TEZOS)
    }

    @Test
    fun `get order by id - flow`() = runBlocking<Unit> {
        val order = randomFlowV1OrderDto()
        val orderId = order.id
        val orderIdFull = OrderIdDto(BlockchainDto.FLOW, orderId.toString()).fullId()

        flowOrderControllerApiMock.mockGetById(order)

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(orderId.toString())
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get all orders - no origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW, BlockchainDto.TEZOS)
        val size = 8

        val flowOrders = listOf(randomFlowV1OrderDto(), randomFlowV1OrderDto())
        val tezosOrders = listOf(randomTezosOrderDto())
        val ethOrders = listOf(randomEthSellOrderDto(), randomEthSellOrderDto())

        coEvery {
            testFlowOrderApi.getOrdersAllByStatus(any(), any(), size, any())
        } returns FlowOrdersPaginationDto(flowOrders, null).toMono()

        tezosOrderControllerApiMock.mockGetAll(tezosOrders)

        coEvery {
            testEthereumOrderApi.getOrdersAllByStatus(any(), any(), size, any())
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        val orders = orderControllerClient.getOrdersAll(
            blockchains, null, size, null, null, null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(5)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get all sync - no origin`() = runBlocking<Unit> {
        val size = 8

        val ethOrders = listOf(randomEthSellOrderDto(), randomEthSellOrderDto())

        coEvery {
            testEthereumOrderApi.getAllSync(any(), any(), size)
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        val orders = orderControllerClient.getAllSync(
            BlockchainDto.ETHEREUM, null, size, null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(2)
        assertThat(orders.continuation).isNull()
    }

    // we removed origin param from api
    @Disabled
    @Test
    fun `get all orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val origin = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val size = 4

        val ethOrders = listOf(randomEthSellOrderDto())
        val polyOrders = listOf(randomEthSellOrderDto(), randomEthSellOrderDto())

        coEvery {
            testEthereumOrderApi.getOrdersAll(origin.value, ethPlatform, any(), size)
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        coEvery {
            testPolygonOrderApi.getOrdersAll(origin.value, ethPlatform, any(), size)
        } returns OrdersPaginationDto(polyOrders, null).toMono()

        val orders = orderControllerClient.getOrdersAll(
            blockchains, null, size, null, null, null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(3)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get all orders - with combined continuation`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val now = nowMillis()
        val ethContinuation = "${now.toEpochMilli()}_${randomString()}"
        val flowContinuation = "${now.toEpochMilli()}_${randomInt()}"
        var continuation = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation,
                BlockchainDto.FLOW.toString() to flowContinuation
            )
        )
        val size = 3

        val ethOrders = listOf(
            randomEthSellOrderDto().copy(lastUpdateAt = now.minusSeconds(2)),
            randomEthSellOrderDto().copy(lastUpdateAt = now.minusSeconds(10))
        )
        val flowOrders = listOf(
            randomFlowV1OrderDto().copy(lastUpdateAt = now.minusSeconds(5)),
            randomFlowV1OrderDto().copy(lastUpdateAt = now.plusSeconds(10))
        )

        coEvery {
            testEthereumOrderApi.getOrdersAllByStatus(any(), ethContinuation, size, any())
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        coEvery {
            testFlowOrderApi.getOrdersAllByStatus(any(), flowContinuation, size, any())
        } returns FlowOrdersPaginationDto(flowOrders, null).toMono()

        val orders = orderControllerClient.getOrdersAll(
            blockchains, continuation.toString(), size, null, null, null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(size)
        assertThat(orders.orders.map { it.id }).contains(
            OrderIdDto(BlockchainDto.ETHEREUM, ethOrders.first().hash.prefixed())
        )
        assertThat(orders.orders.map { it.id }).contains(
            OrderIdDto(BlockchainDto.FLOW, flowOrders.first().id.toString())
        )
        assertThat(orders.orders.map { it.id }).contains(
            OrderIdDto(BlockchainDto.FLOW, flowOrders.last().id.toString())
        )

        assertThat(orders.continuation).isNotNull()
        continuation = CombinedContinuation.parse(orders.continuation)
        assertThat(continuation.continuations[BlockchainDto.ETHEREUM.name]).isNotEqualTo(ArgSlice.COMPLETED)
        assertThat(continuation.continuations[BlockchainDto.FLOW.name]).isEqualTo(ArgSlice.COMPLETED)
    }

    @Test
    fun `get order bids by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val (contract, tokenId) = CompositeItemIdParser.split(ethItemId.value)
        val maker = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())

        val order = randomEthBidOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)

        val ethOrders = listOf(order)

        coEvery {
            testEthereumOrderApi.getOrderBidsByItemAndByStatus(
                contract,
                tokenId.toString(),
                listOf(EthConverter.convertToAddress(maker.value)),
                null,
                ethPlatform,
                continuation,
                size,
                emptyList(),
                unionOrder.bidCurrencyId(),
                null,
                null
            )
        } returns OrdersPaginationDto(ethOrders, continuation).toMono()

        coEvery {
            testEthereumOrderApi.getCurrenciesByBidOrdersOfItem(contract, tokenId.toString(), emptyList())
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.BID,
            listOf(Erc20AssetTypeDto(Address.apply(unionOrder.bidCurrencyId())))
        ).toMono()

        val orders = orderControllerClient.getOrderBidsByItem(
            ethItemId.fullId(),
            platform,
            listOf(maker.fullId()),
            null,
            null,
            null,
            null,
            null,
            continuation,
            size,
            null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get order bids by item - flow`() = runBlocking<Unit> {
        // TODO - implement when Flow support this method
    }

    @Test
    fun `get order bids by maker - flow`() = runBlocking<Unit> {
        // TODO - implement when Flow support this method
    }

    @Test
    fun `get sell orders - no origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW, BlockchainDto.TEZOS)
        val continuation = "${nowMillis().toEpochMilli()}_${randomString()}"
        val size = 4

        val flowOrders = listOf(randomFlowV1OrderDto(), randomFlowV1OrderDto())
        val tezosOrders = listOf(randomTezosOrderDto())
        val ethOrders = listOf(randomEthSellOrderDto(), randomEthSellOrderDto())

        coEvery {
            testFlowOrderApi.getSellOrders(null, continuation, size)
        } returns FlowOrdersPaginationDto(flowOrders, null).toMono()

        coEvery {
            testEthereumOrderApi.getSellOrders(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        val orders = orderControllerClient.getSellOrders(
            blockchains, platform, null, continuation, size, null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(4)
        assertThat(orders.continuation).isNotNull()
    }

    @Test
    fun `get sell orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val origin = randomFlowAddress()
        val size = 2

        val flowOrders = listOf(randomFlowV1OrderDto())

        coEvery {
            testFlowOrderApi.getSellOrders(origin.value, continuation, size)
        } returns FlowOrdersPaginationDto(flowOrders, null).toMono()

        val orders = orderControllerClient.getSellOrders(
            blockchains, platform, origin.fullId(), continuation, size, null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val (contract, tokenId) = CompositeItemIdParser.split(ethItemId.value)
        val maker = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())

        val order = randomEthSellOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)

        val ethOrders = listOf(order)

        coEvery {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                contract,
                tokenId.toString(),
                maker.value,
                null,
                ethPlatform,
                continuation,
                size,
                listOf(com.rarible.protocol.dto.OrderStatusDto.CANCELLED),
                unionOrder.sellCurrencyId()
            )
        } returns OrdersPaginationDto(ethOrders, continuation).toMono()

        coEvery {
            testEthereumOrderApi.getCurrenciesBySellOrdersOfItem(
                contract,
                tokenId.toString(),
                listOf(com.rarible.protocol.dto.OrderStatusDto.CANCELLED)
            )
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.SELL,
            listOf(Erc20AssetTypeDto(Address.apply(unionOrder.sellCurrencyId())))
        ).toMono()

        val orders = orderControllerClient.getSellOrdersByItem(
            ethItemId.fullId(),
            platform,
            maker.fullId(),
            null,
            listOf(OrderStatusDto.CANCELLED),
            continuation,
            size,
            null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders by item - only active`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val (contract, tokenId) = CompositeItemIdParser.split(ethItemId.value)
        val maker = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())

        val order = randomEthSellOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)
        val shortOrder = ShortOrderConverter.convert(unionOrder)

        val ethOrders = listOf(order)
        val shortItem = randomShortItem(ethItemId).copy(
            bestSellOrder = shortOrder,
            bestSellOrders = mapOf(unionOrder.sellCurrencyId() to shortOrder)
        )
        enrichmentItemService.save(shortItem)

        coEvery {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                contract,
                tokenId.toString(),
                maker.value,
                null,
                ethPlatform,
                continuation,
                size,
                listOf(com.rarible.protocol.dto.OrderStatusDto.ACTIVE),
                unionOrder.sellCurrencyId()
            )
        } returns OrdersPaginationDto(ethOrders, continuation).toMono()

        val orders = orderControllerClient.getSellOrdersByItem(
            ethItemId.fullId(),
            platform,
            maker.fullId(),
            null,
            listOf(OrderStatusDto.ACTIVE),
            continuation,
            size,
            null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders order by maker - polygon`() = runBlocking<Unit> {
        val maker = randomPolygonAddress()

        val polygonOrders = listOf(randomEthSellOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrdersByMakerAndByStatus(
                listOf(EthConverter.convertToAddress(maker.value)), null, ethPlatform, continuation, size, emptyList()
            )
        } returns OrdersPaginationDto(polygonOrders, continuation).toMono()

        val orders = orderControllerClient.getSellOrdersByMaker(
            listOf(maker.fullId()),
            null,
            platform,
            null,
            continuation,
            size,
            null,
            null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders order by maker - multiple blockchains`() = runBlocking<Unit> {
        val maker1 = randomPolygonAddress()
        val maker2 = randomFlowAddress()

        val polygonOrders = listOf(randomEthSellOrderDto())
        val flowOrders = listOf(randomFlowV1OrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrdersByMakerAndByStatus(
                listOf(EthConverter.convertToAddress(maker1.value)), null, ethPlatform, continuation, size, emptyList()
            )
        } returns OrdersPaginationDto(polygonOrders, continuation).toMono()

        coEvery {
            testFlowOrderApi.getSellOrdersByMaker(listOf(maker2.value), null, continuation, size)
        } returns FlowOrdersPaginationDto(flowOrders, continuation).toMono()

        val orders = orderControllerClient.getSellOrdersByMaker(
            listOf(maker1.fullId(), maker2.fullId()),
            null,
            platform,
            null,
            continuation,
            size,
            null,
            null
        ).awaitFirst()

        assertThat(orders.orders.map { it.id.value }).containsExactlyInAnyOrder(
            polygonOrders[0].hash.prefixed(),
            flowOrders[0].id.toString()
        )
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders by item - multiple blockchain specified`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val maker = randomFlowAddress()

        val result = orderControllerClient.getSellOrdersByItem(
            ethItemId.fullId(),
            platform,
            maker.fullId(),
            null,
            null,
            continuation,
            size,
            null
        ).awaitFirst()

        // Should be 0 without sub-requests
        assertThat(result.orders).hasSize(0)
    }

    @Test
    fun `get amm trade info - sudo swap`() = runBlocking<Unit> {
        val orderId = randomWord()
        val fullOrderId = OrderIdDto(BlockchainDto.ETHEREUM, orderId)
        val price = randomAmmPriceInfoDto(
            priceValue = BigDecimal("123.54"),
            priceUsd = BigDecimal("34534.345"),
        )

        coEvery {
            testEthereumOrderApi.getAmmBuyInfo(orderId, 1)
        } returns AmmTradeInfoDto(listOf(price)).toMono()

        val result = orderControllerClient.getAmmOrderTradeInfo(fullOrderId.fullId(), 1)
            .awaitSingle() as SudoSwapTradeInfoDto

        val convertedPrice = result.prices[0]

        assertThat(result.orderId).isEqualTo(fullOrderId)
        assertThat(result.prices).hasSize(1)
        assertThat(convertedPrice.price).isEqualTo(price.price)
        assertThat(convertedPrice.priceUsd).isEqualTo(price.priceUsd)
        assertThat(convertedPrice.priceValue).isEqualTo(price.priceValue)
    }
}
