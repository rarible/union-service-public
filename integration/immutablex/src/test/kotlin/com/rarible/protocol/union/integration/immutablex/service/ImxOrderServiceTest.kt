package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxOrderBuySide
import com.rarible.protocol.union.integration.data.randomImxOrderSellSide
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrdersPage
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import randomItemId

class ImxOrderServiceTest {

    private val orderClient: ImxOrderClient = mockk()

    private val service = ImxOrderService(orderClient)
    private val currencyProbeBatchSize = 64

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderClient)
    }

    @Test
    fun `get sell currencies`() = runBlocking<Unit> {
        val token = randomAddress().prefixed()
        val tokenId = randomBigInt().toString()

        val sell = randomImxOrder()
        val swap = randomImxOrder(buy = randomImxOrderSellSide())
        val buy = randomImxOrder(buy = randomImxOrderSellSide(), sell = randomImxOrderBuySide())

        val all = listOf(buy, sell, swap)
        coEvery {
            orderClient.getSellOrdersByItem(token, tokenId, OrderStatusDto.ACTIVE, null, currencyProbeBatchSize)
        } returns ImmutablexOrdersPage.empty()

        coEvery {
            orderClient.getSellOrdersByItem(token, tokenId, null, null, currencyProbeBatchSize)
        } returns ImmutablexOrdersPage(all)

        val result = service.getSellCurrencies("$token:$tokenId")

        // we are interested only in sell-orders currencies, swap/buy orders should be filtered
        assertThat(result).hasSize(1)
        assertThat(result[0].currencyId()).isEqualTo(sell.buy.data.tokenAddress)
    }

    @Test
    fun `get bid currencies`() = runBlocking<Unit> {
        val token = randomAddress().prefixed()
        val tokenId = randomBigInt().toString()

        val sell = randomImxOrder(buy = randomImxOrderBuySide())
        val swap = randomImxOrder(buy = randomImxOrderSellSide())
        val buy = randomImxOrder(buy = randomImxOrderSellSide(), sell = randomImxOrderBuySide())

        val all = listOf(buy, sell, swap)

        coEvery {
            orderClient.getBuyOrdersByItem(token, tokenId, null, currencyProbeBatchSize)
        } returns ImmutablexOrdersPage(all)

        val result = service.getBidCurrencies("$token:$tokenId")

        // we are interested only in buy-orders currencies, swap/sell orders should be filtered
        assertThat(result).hasSize(1)
        assertThat(result[0].currencyId()).isEqualTo(buy.sell.data.tokenAddress)
    }

    @Test
    fun `getOrderBidsByMaker - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getOrderBidsByMaker(
            null,
            listOf(randomString()),
            origin,
            null,
            null,
            null,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getOrderBidsByItem - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getOrderBidsByItem(
            null,
            randomItemId(BlockchainDto.IMMUTABLEX).value,
            null,
            origin,
            null,
            null,
            null,
            randomString(),
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrders - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrders(
            null,
            origin,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrdersByCollection - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrdersByCollection(
            null,
            randomString(),
            origin,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrdersByItem - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrdersByItem(
            null,
            randomItemId(BlockchainDto.IMMUTABLEX).value,
            null,
            origin,
            null,
            randomString(),
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrdersByMaker - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrdersByMaker(
            null,
            listOf(randomString()),
            origin,
            null,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }
}