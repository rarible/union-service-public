package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.HoldNftItemIdsDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.order.api.client.OrderAdminControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOpenSeaV1OrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthOrderServiceTest {

    private val orderControllerApi: OrderControllerApi = mockk()
    private val orderAdminControllerApi: OrderAdminControllerApi = mockk()
    private val converter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val service = EthereumOrderService(orderControllerApi, orderAdminControllerApi, converter)

    @Test
    fun `ethereum get all`() = runBlocking<Unit> {
        val order1 = randomEthSellOrderDto()
        val order2 = randomEthV2OrderDto()

        val continuation = randomString()
        val size = randomInt()

        val expected1 = converter.convert(order1, BlockchainDto.ETHEREUM)
        val expected2 = converter.convert(order2, BlockchainDto.ETHEREUM)

        coEvery {
            orderControllerApi.getOrdersAllByStatus(any(), continuation, size, any())
        } returns OrdersPaginationDto(listOf(order1, order2)).toMono()

        val result = service.getOrdersAll(
            continuation,
            size,
            null,
            null
        )

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(expected1)
        assertThat(result.entities[1]).isEqualTo(expected2)
    }

    @Test
    fun `ethereum get all - skip when only historical status is given`() = runBlocking<Unit> {
        // given
        val continuation = randomString()
        val size = randomInt()

        // when
        val result = service.getOrdersAll(
            continuation,
            size,
            null,
            listOf(OrderStatusDto.HISTORICAL)
        )

        // then
        assertThat(result.entities).isEmpty()
        assertThat(result.continuation).isNull()
        confirmVerified(orderControllerApi)
    }

    @Test
    fun `ethereum get by id`() = runBlocking<Unit> {
        val order = randomEthOpenSeaV1OrderDto()
        val orderId = EthConverter.convert(order.hash)
        val expected = converter.convert(order, BlockchainDto.ETHEREUM)

        coEvery { orderControllerApi.getOrderByHash(orderId) } returns order.toMono()

        val result = service.getOrderById(orderId)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ethereum get amm orders by item`() = runBlocking<Unit> {
        val order = randomEthV2OrderDto()

        val contract = randomAddressString()
        val tokenId = randomBigInt()
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, contract, tokenId)
        val continuation = randomString()
        val size = 73

        coEvery {
            orderControllerApi.getAmmOrdersByItem(
                contract,
                tokenId.toString(),
                continuation,
                size
            )
        } returns OrdersPaginationDto(listOf(order), null).toMono()

        val result = service.getAmmOrdersByItem(
            itemId.value,
            continuation,
            size
        )

        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0].id.value).isEqualTo(order.hash.prefixed())
    }

    @Test
    fun `ethereum get amm order item ids`() = runBlocking<Unit> {
        val orderId = randomWord()
        val continuation = randomString()
        val size = 33

        val itemId = randomEthItemId()

        coEvery {
            orderControllerApi.getAmmOrderItemIds(orderId, continuation, size)
        } returns HoldNftItemIdsDto(listOf(itemId.value), null).toMono()

        val result = service.getAmmOrderItemIds(orderId, continuation, size)

        assertThat(result.entities).isEqualTo(listOf(itemId))
    }

    @Test
    fun `cancel order - ok`() = runBlocking<Unit> {
        val orderId = randomWord()
        val canceledOrder = randomEthV2OrderDto()

        coEvery {
            orderAdminControllerApi.changeState(orderId, any())
        } returns canceledOrder

        val result = service.cancelOrder(orderId)

        assertThat(result.id.value).isEqualTo(EthConverter.convert(canceledOrder.hash))
    }
}
