package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.common.nowMillis
import com.rarible.dipdup.client.OrderClient
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.model.DipDupOrdersPage
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
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
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZoneOffset
import java.util.*

class TezosOrderServiceTest {

    private val currencyService: CurrencyService = CurrencyMock.currencyServiceMock
    private val dipdupOrderClient: OrderClient = mockk()

    private val dipdupOrderConverter = DipDupOrderConverter(currencyService)
    private val dipdupOrderService = DipdupOrderServiceImpl(dipdupOrderClient, dipdupOrderConverter, DipDupIntegrationProperties.Marketplaces())
    private val service = TezosOrderService(dipdupOrderService)

    @BeforeEach
    fun beforeEach() {
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

    private fun dipDupOrder(orderId: String): DipDupOrder {
        return DipDupOrder(
            id = orderId,
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.HEN,
            payouts = emptyList(),
            originFees = emptyList(),
            status = OrderStatus.ACTIVE,
            startAt = null,
            endedAt = null,
            endAt = null,
            lastUpdatedAt = nowMillis().atOffset(ZoneOffset.UTC),
            createdAt = nowMillis().atOffset(ZoneOffset.UTC),
            maker = UUID.randomUUID().toString(),
            makePrice = null,
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
            takePrice = null,
            cancelled = false,
            salt = BigInteger.ONE
        )
    }
}
