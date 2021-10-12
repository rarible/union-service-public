package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.test.data.randomEthAddress
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import com.rarible.protocol.union.test.data.randomPolygonAddress
import com.rarible.protocol.union.test.data.randomUnionItem
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

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

        coEvery { testEthereumOrderApi.getOrderByHash(orderId) } returns order.toMono()

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(orderId)
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get order by id - polygon`() = runBlocking<Unit> {
        val order = randomEthLegacyOrderDto()
        val orderId = EthConverter.convert(order.hash)
        val orderIdFull = OrderIdDto(BlockchainDto.POLYGON, order.hash.prefixed()).fullId()

        coEvery { testPolygonOrderApi.getOrderByHash(orderId) } returns order.toMono()

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(orderId)
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.POLYGON)
    }

    @Test
    fun `get order by id - flow`() = runBlocking<Unit> {
        val order = randomFlowV1OrderDto()
        val orderId = order.id
        val orderIdFull = OrderIdDto(BlockchainDto.FLOW, orderId.toString()).fullId()

        coEvery { testFlowOrderApi.getOrderByOrderId(orderId.toString()) } returns order.toMono()

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()

        assertThat(unionOrder.id.value).isEqualTo(orderId.toString())
        assertThat(unionOrder.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get all orders - no origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 3

        val polygonOwnerships = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())
        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getOrdersAll(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(polygonOwnerships, this@OrderControllerFt.continuation).toMono()

        coEvery {
            testEthereumOrderApi.getOrdersAll(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, this@OrderControllerFt.continuation).toMono()

        val orders = orderControllerClient.getOrdersAll(
            blockchains, platform, null, continuation, size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(3)
        assertThat(orders.continuation).isNotNull()
    }

    @Test
    fun `get all orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
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
        val ethItem = randomUnionItem(ethItemId)

        val contract = ethItemId.token
        val tokenId = ethItemId.tokenId
        val maker = randomEthAddress()

        val order = randomEthLegacyOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)
        val shortOrder = ShortOrderConverter.convert(unionOrder)
        val shortItem = ShortItemConverter.convert(ethItem).copy(
            bestBidOrder = shortOrder,
            bestBidOrders = mapOf(unionOrder.bidCurrencyId to shortOrder)
        )
        enrichmentItemService.save(shortItem)

        val ethOrders = listOf(order)

        coEvery {
            testEthereumOrderApi.getOrderBidsByItemAndByStatus(
                contract.value,
                tokenId.toString(),
                null,
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
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 3

        val polygonOwnerships = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())
        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrders(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(polygonOwnerships, this@OrderControllerFt.continuation).toMono()

        coEvery {
            testEthereumOrderApi.getSellOrders(null, ethPlatform, continuation, size)
        } returns OrdersPaginationDto(ethOrders, this@OrderControllerFt.continuation).toMono()

        val orders = orderControllerClient.getSellOrders(
            blockchains, platform, null, continuation, size
        ).awaitFirst()

        assertThat(orders.orders).hasSize(3)
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
        } returns OrdersPaginationDto(ethOrders, this@OrderControllerFt.continuation).toMono()

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
        val ethItem = randomUnionItem(ethItemId)

        val contract = ethItemId.token
        val tokenId = ethItemId.tokenId
        val maker = randomEthAddress()

        val order = randomEthLegacyOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)
        val shortOrder = ShortOrderConverter.convert(unionOrder)
        val shortItem = ShortItemConverter.convert(ethItem).copy(
            bestSellOrder = shortOrder,
            bestSellOrders = mapOf(unionOrder.sellCurrencyId to shortOrder)
        )
        enrichmentItemService.save(shortItem)

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
                null,
                unionOrder.sellCurrencyId
            )
        } returns OrdersPaginationDto(ethOrders, continuation).toMono()

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

        // TODO add specific responce check after Advise beign implemented
        catchThrowable {
            runBlocking {
                orderControllerClient.getSellOrdersByItem(
                    contract.fullId(),
                    tokenId.toString(),
                    platform,
                    maker.fullId(),
                    null,
                    null,
                    continuation,
                    size
                ).awaitFirst()
            }
        }
    }
}
