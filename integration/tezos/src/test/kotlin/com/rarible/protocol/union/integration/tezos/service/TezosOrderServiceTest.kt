package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.dipdup.client.OrderClient
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.model.DipDupOrdersPage
import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.tezos.dto.OrderPaginationDto
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.integration.tezos.TezosIntegrationProperties
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderServiceImpl
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class TezosOrderServiceTest {

    private val currencyService: CurrencyService = CurrencyMock.currencyServiceMock
    private val orderControllerApi: OrderControllerApi = mockk()
    private val dipdupOrderClient: OrderClient = mockk()

    private val tezosOrderConverter = TezosOrderConverter(currencyService)
    private val dipdupOrderConverter = DipDupOrderConverter(currencyService)
    private val dipdupOrderService = DipdupOrderServiceImpl(dipdupOrderClient, dipdupOrderConverter)
    private val tezosIntegrationProperties = TezosIntegrationProperties(
        enabled = true,
        consumer = null,
        client = null,
        daemon = DaemonWorkerProperties(),
        auctionContracts = null,
        origins = emptyMap(),
        showLegacyOrders = true
    )
    private val service = TezosOrderService(orderControllerApi, tezosOrderConverter, dipdupOrderService, tezosIntegrationProperties)

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderControllerApi)
        clearMocks(dipdupOrderClient)
    }

    @Test
    fun `should return dipdup order by id`() = runBlocking<Unit> {
        val orderId = "ca5418bd-92aa-529c-91fa-c670a2d2d878"
        val dipDupOrder = dipDupOrder(orderId)
        coEvery { dipdupOrderClient.getOrderById(orderId) } returns dipDupOrder

        val order = service.getOrderById(orderId)
        assertThat(order.id).isEqualTo(OrderIdDto(BlockchainDto.TEZOS, orderId))
    }

    @Test
    fun `should return dipdup orders by ids`() = runBlocking<Unit> {
        val orderId = "ca5418bd-92aa-529c-91fa-c670a2d2d878"
        val dipDupOrder = dipDupOrder(orderId)
        coEvery { dipdupOrderClient.getOrdersByIds(listOf(orderId)) } returns listOf(dipDupOrder)
        coEvery { orderControllerApi.getOrderByIds(any()) } returns Flux.empty()

        val order =
            service.getOrdersByIds(listOf(orderId, "fc6a7fd11a58706b78731729c739bd9a0246b5fc3f69eed1f1fe1e1cc4269cde"))
        assertThat(order).hasSize(1)
        assertThat(order.first().id).isEqualTo(OrderIdDto(BlockchainDto.TEZOS, orderId))
    }

    @Test
    fun `should return dipdup orders via ordersAll`() = runBlocking<Unit> {
        val orderId = "ca5418bd-92aa-529c-91fa-c670a2d2d878"
        val continuation = "1650622934000_$orderId"

        val dipDupOrder = dipDupOrder(orderId)
        coEvery {
            dipdupOrderClient.getOrdersAll(
                any(),
                any(),
                any(),
                any()
            )
        } returns DipDupOrdersPage(orders = listOf(dipDupOrder), continuation = continuation)

        val orders = service.getOrdersAll(
            sort = null,
            continuation = continuation,
            status = null,
            size = 1
        )
        assertThat(orders.entities).hasSize(1)
        assertThat(orders.entities.first().id.value).isEqualTo(dipDupOrder.id)
        assertThat(orders.continuation).isEqualTo(continuation)
    }

    @Test
    fun `should return legacy + dipdup orders via ordersAll`() = runBlocking<Unit> {
        val orderId = "ca5418bd-92aa-529c-91fa-c670a2d2d878"
        val continuation = "1650622934000_$orderId"

        val order = randomTezosOrderDto()
        coEvery {
            orderControllerApi.getOrdersAll(
                any(), any(), any(), any(), any()
            )
        } returns Mono.just(OrderPaginationDto(listOf(order), null))

        val dipDupOrder = dipDupOrder(orderId)
        coEvery {
            dipdupOrderClient.getOrdersAll(
                any(),
                any(),
                any(),
                any()
            )
        } returns DipDupOrdersPage(orders = listOf(dipDupOrder), continuation = continuation)

        val orders = service.getOrdersAll(
            sort = null,
            continuation = "1650622934000_0b375429527dca45f800cee0847b36a4a3320c63858bd12d12f4621b8509bbf6",
            status = null,
            size = 2
        )
        assertThat(orders.entities).hasSize(2)
        assertThat(orders.entities.first().id.value).isEqualTo(order.hash)
        assertThat(orders.entities.last().id.value).isEqualTo(dipDupOrder.id)
        assertThat(orders.continuation).isEqualTo(continuation)
    }

    @Test
    fun `should return dipdup orders by item`() = runBlocking<Unit> {
        val token = "test"
        val tokenId = BigInteger.valueOf(123)
        val orderId = "ca5418bd-92aa-529c-91fa-c670a2d2d878"
        val continuation = "1650622934000_$orderId"

        val dipDupOrder = dipDupOrder(orderId)
        coEvery {
            dipdupOrderClient.getOrdersByItem(
                contract = token,
                tokenId = tokenId.toString(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns DipDupOrdersPage(orders = listOf(dipDupOrder), continuation = continuation)

        val orders = service.getSellOrdersByItem(
            platform = null,
            itemId = "test:123",
            currencyId = "",
            origin = null,
            maker = null,
            continuation = continuation,
            status = null,
            size = 1
        )
        assertThat(orders.entities).hasSize(1)
        assertThat(orders.entities.first().id.value).isEqualTo(dipDupOrder.id)
        assertThat(orders.continuation).isEqualTo(continuation)
    }

    @Test
    fun `should return legacy + dipdup orders by item`() = runBlocking<Unit> {
        val token = "test"
        val tokenId = BigInteger.valueOf(123)
        val orderId = "ca5418bd-92aa-529c-91fa-c670a2d2d878"
        val continuation = "1650622934000_$orderId"

        val order = randomTezosOrderDto()
        coEvery {
            orderControllerApi.getSellOrderByItem(
                token,
                tokenId.toString(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns Mono.just(OrderPaginationDto(listOf(order), null))

        val dipDupOrder = dipDupOrder(orderId)
        coEvery {
            dipdupOrderClient.getOrdersByItem(
                contract = token,
                tokenId = tokenId.toString(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns DipDupOrdersPage(orders = listOf(dipDupOrder), continuation = continuation)

        val orders = service.getSellOrdersByItem(
            platform = null,
            itemId = "test:123",
            currencyId = "",
            origin = null,
            maker = null,
            continuation = "1650622934000_0b375429527dca45f800cee0847b36a4a3320c63858bd12d12f4621b8509bbf6",
            status = null,
            size = 2
        )
        assertThat(orders.entities).hasSize(2)
        assertThat(orders.entities.first().id.value).isEqualTo(order.hash)
        assertThat(orders.entities.last().id.value).isEqualTo(dipDupOrder.id)
        assertThat(orders.continuation).isEqualTo(continuation)
    }

    private fun dipDupOrder(orderId: String): DipDupOrder {
        return DipDupOrder(
            id = orderId,
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.Hen,
            payouts = emptyList(),
            originFees = emptyList(),
            status = OrderStatus.ACTIVE,
            startAt = null,
            endedAt = null,
            endAt = null,
            lastUpdatedAt = Instant.now().atOffset(ZoneOffset.UTC),
            createdAt = Instant.now().atOffset(ZoneOffset.UTC),
            maker = UUID.randomUUID().toString(),
            make = Asset(
                assetType = Asset.NFT(
                    contract = UUID.randomUUID().toString(),
                    tokenId = BigInteger.ONE
                ),
                assetValue = BigDecimal.ONE
            ),
            taker = null,
            take = Asset(
                assetType = Asset.XTZ(),
                assetValue = BigDecimal.ONE
            ),
            cancelled = false,
            salt = BigInteger.ONE
        )
    }
}
