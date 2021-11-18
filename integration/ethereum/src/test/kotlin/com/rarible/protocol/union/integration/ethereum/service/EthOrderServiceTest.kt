package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOpenSeaV1OrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthOrderServiceTest {

    private val orderControllerApi: OrderControllerApi = mockk()
    private val converter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val service = EthereumOrderService(orderControllerApi, converter)

    @Test
    fun `ethereum get all`() = runBlocking<Unit> {
        val order1 = randomEthLegacyOrderDto()
        val order2 = randomEthV2OrderDto()

        val origin = randomString()
        val continuation = randomString()
        val size = randomInt()

        val expected1 = converter.convert(order1, BlockchainDto.ETHEREUM)
        val expected2 = converter.convert(order2, BlockchainDto.ETHEREUM)

        coEvery {
            orderControllerApi.getOrdersAll(origin, PlatformDto.RARIBLE, continuation, size)
        } returns OrdersPaginationDto(listOf(order1, order2)).toMono()

        val result = service.getOrdersAll(
            com.rarible.protocol.union.dto.PlatformDto.RARIBLE,
            origin,
            continuation,
            size
        )

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(expected1)
        assertThat(result.entities[1]).isEqualTo(expected2)
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

}