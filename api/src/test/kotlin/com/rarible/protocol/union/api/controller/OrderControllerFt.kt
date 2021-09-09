package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.test.data.*
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

    private val DEF_CONTINUATION = null
    private val DEF_SIZE = 5
    private val DEF_PLATFORM = PlatformDto.ALL
    private val DEF_ETH_PLATFORM = com.rarible.protocol.dto.PlatformDto.ALL

    @Autowired
    lateinit var orderControllerClient: OrderControllerApi

    @Test
    fun `get order by id - ethereum`() = runBlocking<Unit> {
        val order = randomEthLegacyOrderDto()
        val orderId = EthConverter.convert(order.hash)
        val orderIdFull = randomEthOrderIdFullValue(order.hash)

        coEvery { testEthereumOrderApi.getOrderByHash(orderId) } returns order.toMono()

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()
        val ethOrder = unionOrder as EthOrderDto

        assertThat(ethOrder.id.value).isEqualTo(orderId)
        assertThat(ethOrder.id.blockchain).isEqualTo(EthBlockchainDto.ETHEREUM)
    }

    @Test
    fun `get order by id - polygon`() = runBlocking<Unit> {
        val order = randomEthLegacyOrderDto()
        val orderId = EthConverter.convert(order.hash)
        val orderIdFull = randomPolygonOrderIdFullValue(order.hash)

        coEvery { testPolygonOrderApi.getOrderByHash(orderId) } returns order.toMono()

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()
        val ethOrder = unionOrder as EthOrderDto

        assertThat(ethOrder.id.value).isEqualTo(orderId)
        assertThat(ethOrder.id.blockchain).isEqualTo(EthBlockchainDto.POLYGON)
    }

    @Test
    fun `get order by id - flow`() = runBlocking<Unit> {
        val order = randomFlowV1OrderDto()
        val orderId = order.id
        val orderIdFull = randomFlowOrderIdFullValue(orderId)

        coEvery { testFlowOrderApi.getOrderByOrderId(orderId.toString()) } returns order.toMono()

        val unionOrder = orderControllerClient.getOrderById(orderIdFull).awaitFirst()
        val ethOrder = unionOrder as FlowOrderDto

        assertThat(ethOrder.id.value).isEqualTo(orderId.toString())
        assertThat(ethOrder.id.blockchain).isEqualTo(FlowBlockchainDto.FLOW)
    }

    @Test
    fun `update order make stock - ethereum`() = runBlocking<Unit> {
        val order = randomEthLegacyOrderDto()
        val orderId = EthConverter.convert(order.hash)
        val orderIdFull = randomEthOrderIdFullValue(order.hash)

        coEvery { testEthereumOrderApi.updateOrderMakeStock(orderId) } returns order.toMono()

        val unionOrder = orderControllerClient.updateOrderMakeStock(orderIdFull).awaitFirst()
        val ethOrder = unionOrder as EthOrderDto

        assertThat(ethOrder.id.value).isEqualTo(orderId)
        assertThat(ethOrder.id.blockchain).isEqualTo(EthBlockchainDto.ETHEREUM)
    }

    @Test
    fun `get all orders - no origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 3

        val polygonOwnerships = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())
        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getOrdersAll(null, DEF_ETH_PLATFORM, continuation, size)
        } returns OrdersPaginationDto(polygonOwnerships, DEF_CONTINUATION).toMono()

        coEvery {
            testEthereumOrderApi.getOrdersAll(null, DEF_ETH_PLATFORM, continuation, size)
        } returns OrdersPaginationDto(ethOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getOrdersAll(
            blockchains, DEF_PLATFORM, null, continuation, size
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(3)
        assertThat(unionOrders.continuation).isNotNull()
    }

    @Test
    fun `get all orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        val continuation = "${nowMillis()}_${randomString()}"
        val origin = randomEthAddress()
        val size = 3

        val ethOrders = listOf(randomEthLegacyOrderDto(), randomEthLegacyOrderDto())

        coEvery {
            testEthereumOrderApi.getOrdersAll(origin.value, DEF_ETH_PLATFORM, continuation, size)
        } returns OrdersPaginationDto(ethOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getOrdersAll(
            blockchains, DEF_PLATFORM, origin.toString(), continuation, size
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(2)
        assertThat(unionOrders.continuation).isNull()
    }

    @Test
    fun `get order bids by item - ethereum`() = runBlocking<Unit> {
        val contract = randomEthAddress()
        val tokenId = randomBigInt()
        val maker = randomEthAddress()

        val ethOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testEthereumOrderApi.getOrderBidsByItem(
                contract.value,
                tokenId.toString(),
                maker.value,
                null,
                DEF_ETH_PLATFORM,
                DEF_CONTINUATION,
                DEF_SIZE
            )
        } returns OrdersPaginationDto(ethOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getOrderBidsByItem(
            contract.toString(),
            tokenId.toString(),
            DEF_PLATFORM,
            maker.toString(),
            null,
            DEF_CONTINUATION,
            DEF_SIZE
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(1)
        assertThat(unionOrders.orders[0]).isInstanceOf(EthOrderDto::class.java)
        assertThat(unionOrders.continuation).isNull()
    }

    @Test
    fun `get order bids by item - flow`() = runBlocking<Unit> {
        // TODO - implement when Flow support this method
    }

    @Test
    fun `get order bids by maker - polygon`() = runBlocking<Unit> {
        val maker = randomPolygonAddress()

        val polygonOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getOrderBidsByMaker(maker.value, null, DEF_ETH_PLATFORM, DEF_CONTINUATION, DEF_SIZE)
        } returns OrdersPaginationDto(polygonOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getOrderBidsByMaker(
            maker.toString(),
            DEF_PLATFORM,
            null,
            DEF_CONTINUATION,
            DEF_SIZE
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(1)
        assertThat(unionOrders.orders[0]).isInstanceOf(EthOrderDto::class.java)
        assertThat(unionOrders.continuation).isNull()
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
            testPolygonOrderApi.getSellOrders(null, DEF_ETH_PLATFORM, continuation, size)
        } returns OrdersPaginationDto(polygonOwnerships, DEF_CONTINUATION).toMono()

        coEvery {
            testEthereumOrderApi.getSellOrders(null, DEF_ETH_PLATFORM, continuation, size)
        } returns OrdersPaginationDto(ethOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getSellOrders(
            blockchains, DEF_PLATFORM, null, continuation, size
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(3)
        assertThat(unionOrders.continuation).isNotNull()
    }

    @Test
    fun `get sell orders - with origin`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        val continuation = "${nowMillis()}_${randomString()}"
        val origin = randomPolygonAddress()
        val size = 1

        val ethOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrders(origin.value, DEF_ETH_PLATFORM, continuation, size)
        } returns OrdersPaginationDto(ethOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getSellOrders(
            blockchains, DEF_PLATFORM, origin.toString(), continuation, size
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(1)
        assertThat(unionOrders.continuation).isNull()
    }

    @Test
    fun `get sell orders by collection - polygon`() = runBlocking<Unit> {
        val collection = randomPolygonAddress()
        val polygonOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrdersByCollection(
                collection.value,
                null,
                DEF_ETH_PLATFORM,
                DEF_CONTINUATION,
                DEF_SIZE
            )
        } returns OrdersPaginationDto(polygonOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getSellOrdersByCollection(
            collection.toString(),
            DEF_PLATFORM,
            null,
            DEF_CONTINUATION,
            DEF_SIZE
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(1)
        assertThat(unionOrders.orders[0]).isInstanceOf(EthOrderDto::class.java)
        assertThat(unionOrders.continuation).isNull()
    }

    @Test
    fun `get sell orders by item - ethereum`() = runBlocking<Unit> {
        val contract = randomEthAddress()
        val tokenId = randomBigInt()
        val maker = randomEthAddress()

        val ethOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testEthereumOrderApi.getSellOrdersByItem(
                contract.value,
                tokenId.toString(),
                maker.value,
                null,
                DEF_ETH_PLATFORM,
                DEF_CONTINUATION,
                DEF_SIZE
            )
        } returns OrdersPaginationDto(ethOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getSellOrdersByItem(
            contract.toString(),
            tokenId.toString(),
            DEF_PLATFORM,
            maker.toString(),
            null,
            DEF_CONTINUATION,
            DEF_SIZE
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(1)
        assertThat(unionOrders.orders[0]).isInstanceOf(EthOrderDto::class.java)
        assertThat(unionOrders.continuation).isNull()
    }

    @Test
    fun `get sell orders order by maker - polygon`() = runBlocking<Unit> {
        val maker = randomPolygonAddress()

        val polygonOrders = listOf(randomEthLegacyOrderDto())

        coEvery {
            testPolygonOrderApi.getSellOrdersByMaker(maker.value, null, DEF_ETH_PLATFORM, DEF_CONTINUATION, DEF_SIZE)
        } returns OrdersPaginationDto(polygonOrders, DEF_CONTINUATION).toMono()

        val unionOrders = orderControllerClient.getSellOrdersByMaker(
            maker.toString(),
            DEF_PLATFORM,
            null,
            DEF_CONTINUATION,
            DEF_SIZE
        ).awaitFirst()

        assertThat(unionOrders.orders).hasSize(1)
        assertThat(unionOrders.orders[0]).isInstanceOf(EthOrderDto::class.java)
        assertThat(unionOrders.continuation).isNull()
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
                    contract.toString(),
                    tokenId.toString(),
                    DEF_PLATFORM,
                    maker.toString(),
                    null,
                    DEF_CONTINUATION,
                    DEF_SIZE
                ).awaitFirst()
            }
        }
    }
}