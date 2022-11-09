package com.rarible.protocol.union.enrichment.service.query.order

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderApiServiceTest {

    private val router: BlockchainRouter<OrderService> = mockk()
    private val orderService: OrderService = mockk()
    private val ethOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val orderApiService = OrderApiMergeService(
        router
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(router, orderService)
        every { router.getService(BlockchainDto.ETHEREUM) } returns orderService
    }

    @Test
    fun `get best bids - no currencies found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { orderService.getBidCurrencies(itemId.value) } returns emptyList()
        val result = getOrderBidsByItem(itemId, null, 10)

        Assertions.assertThat(result.continuation).isNull()
        Assertions.assertThat(result.orders).hasSize(0)
    }

    @Test
    fun `get best sells - no currencies found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { orderService.getSellCurrencies(itemId.value) } returns emptyList()
        val result = getSellOrdersByItem(itemId, null, 10)

        Assertions.assertThat(result.continuation).isNull()
        Assertions.assertThat(result.orders).hasSize(0)
    }

    @Test
    fun `get best bids - completed not requested`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethOrder = randomEthBidOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)

        coEvery {
            orderService.getBidCurrencies(ethItemId.value)
        } returns listOf(
            EthErc20AssetTypeDto(
                ContractAddressConverter.convert(
                    ethItemId.blockchain,
                    unionOrder.bidCurrencyId
                )
            )
        )

        val result = getOrderBidsByItem(ethItemId, "${unionOrder.bidCurrencyId}:COMPLETED", 10)

        Assertions.assertThat(result.continuation).isNull()
        Assertions.assertThat(result.orders).hasSize(0)
    }

    private suspend fun getOrderBidsByItem(itemId: ItemIdDto, continuation: String?, size: Int): OrdersDto {
        return orderApiService.getOrderBidsByItem(
            itemId = itemId,
            platform = PlatformDto.RARIBLE,
            maker = null,
            origin = null,
            status = emptyList(),
            start = null,
            end = null,
            continuation = continuation,
            size = size
        )
    }

    private suspend fun getSellOrdersByItem(itemId: ItemIdDto, continuation: String?, size: Int): OrdersDto {
        return orderApiService.getSellOrdersByItem(
            itemId = itemId,
            platform = PlatformDto.RARIBLE,
            maker = null,
            origin = null,
            status = null,
            continuation = continuation,
            size = size
        )
    }

}