package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.tezos.dto.OrderPaginationDto
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@FlowPreview
@IntegrationTest
class OrderControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ORDER.default
    private val platform = PlatformDto.ALL
    private val ethPlatform = com.rarible.protocol.dto.PlatformDto.ALL

    @Autowired
    lateinit var orderControllerClient: OrderControllerApi

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Test
    fun `get order by id - ethereum`() = runBlocking<Unit> {
        val order = randomEthLegacyOrderDto()
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
        val orderIdFull = OrderIdDto(BlockchainDto.TEZOS, order.hash).fullId()

        tezosOrderControllerApiMock.mockGetById(order)

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(order.hash)
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
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 8

        val flowOrders = listOf(randomFlowV1OrderDto(), randomFlowV1OrderDto())
        val tezosOrders = listOf(randomTezosOrderDto())
        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testFlowOrderApi.getOrdersAll(null, continuation, size)
        } returns FlowOrdersPaginationDto(flowOrders, null).toMono()

        coEvery {
            testTezosOrderApi.getOrdersAll(null, size, continuation)
        } returns OrderPaginationDto(tezosOrders, null).toMono()

        coEvery {
            testEthereumOrderApi.getOrdersAll(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        val orders = orderControllerClient.getOrdersAll(
            blockchains, platform, null, continuation, size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(5)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get all orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val origin = randomEthAddress()
        val size = 3

        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testEthereumOrderApi.getOrdersAll(origin.value, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, this@OrderControllerFt.continuation).toMono()

        val orders = orderControllerClient.getOrdersAll(
            blockchains, platform, origin.fullId(), continuation, size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(2)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get order bids by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val contract = ethItemId.token
        val tokenId = ethItemId.tokenId
        val maker = randomEthAddress()

        val order = randomEthLegacyOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)

        val ethOrders = listOf(order)

        coEvery {
            testEthereumOrderApi.getOrderBidsByItemAndByStatus(
                contract.value,
                tokenId.toString(),
                emptyList(),
                maker.value,
                null,
                ethPlatform,
                continuation,
                size,
                unionOrder.bidCurrencyId,
                null,
                null
            )
        } returns OrdersPaginationDto(ethOrders, continuation).toMono()

        coEvery {
            testEthereumOrderApi.getCurrenciesByBidOrdersOfItem(contract.value, tokenId.toString())
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.BID,
            listOf(Erc20AssetTypeDto(Address.apply(unionOrder.bidCurrencyId)))
        ).toMono()

        val orders = orderControllerClient.getOrderBidsByItem(
            contract.fullId(),
            tokenId.toString(),
            platform,
            maker.fullId(),
            null,
            null,
            null,
            null,
            continuation,
            size
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
        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testFlowOrderApi.getSellOrders(null, continuation, size)
        } returns FlowOrdersPaginationDto(flowOrders, null).toMono()

        coEvery {
            testTezosOrderApi.getSellOrders(null, size, continuation)
        } returns OrderPaginationDto(tezosOrders, null).toMono()

        coEvery {
            testEthereumOrderApi.getSellOrders(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        val orders = orderControllerClient.getSellOrders(
            blockchains, platform, null, continuation, size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(4)
        assertThat(orders.continuation).isNotNull()
    }

    @Test
    fun `get sell orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        val continuation = "${nowMillis()}_${randomString()}"
        val origin = randomPolygonAddress()
        val size = 1

        val ethOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrders(origin.value, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, null).toMono()

        val orders = orderControllerClient.getSellOrders(
            blockchains, platform, origin.fullId(), continuation, size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders by collection - polygon`() = runBlocking<Unit> {
        val collection = randomPolygonAddress()
        val polygonOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrdersByCollection(
                collection.value,
                null,
                ethPlatform,
                continuation,
                size
            )
        } returns OrdersPaginationDto(polygonOrders, continuation).toMono()

        val orders = orderControllerClient.getSellOrdersByCollection(
            collection.fullId(),
            platform,
            null,
            continuation,
            size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val contract = ethItemId.token
        val tokenId = ethItemId.tokenId
        val maker = randomEthAddress()

        val order = randomEthLegacyOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)

        val ethOrders = listOf(order)

        coEvery {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                contract.value,
                tokenId.toString(),
                maker.value,
                null,
                ethPlatform,
                continuation,
                size,
                emptyList(),
                unionOrder.sellCurrencyId
            )
        } returns OrdersPaginationDto(ethOrders, continuation).toMono()

        coEvery {
            testEthereumOrderApi.getCurrenciesBySellOrdersOfItem(contract.value, tokenId.toString())
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.SELL,
            listOf(Erc20AssetTypeDto(Address.apply(unionOrder.sellCurrencyId)))
        ).toMono()

        val orders = orderControllerClient.getSellOrdersByItem(
            contract.fullId(),
            tokenId.toString(),
            platform,
            maker.fullId(),
            null,
            null,
            continuation,
            size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders order by maker - polygon`() = runBlocking<Unit> {
        val maker = randomPolygonAddress()

        val polygonOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrdersByMaker(maker.value, null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(polygonOrders, continuation).toMono()

        val orders = orderControllerClient.getSellOrdersByMaker(
            maker.fullId(),
            platform,
            null,
            continuation,
            size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `get sell orders by item - multiple blockchain specified`() = runBlocking<Unit> {
        val contract = randomEthAddress()
        val tokenId = randomBigInt()
        val maker = randomPolygonAddress()

        val result = orderControllerClient.getSellOrdersByItem(
            contract.fullId(),
            tokenId.toString(),
            platform,
            maker.fullId(),
            null,
            null,
            continuation,
            size
        ).awaitFirst()

        // Should be 0 without sub-requests
        assertThat(result.orders).hasSize(0)
    }
}
