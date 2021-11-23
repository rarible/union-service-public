package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderApiServiceTest {

    private val router: BlockchainRouter<OrderService> = mockk()
    private val orderService: OrderService = mockk()
    private val ethOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val orderApiService = OrderApiService(
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
        coEvery { orderService.getBidCurrencies(itemId.contract, itemId.tokenId.toString()) } returns emptyList()
        val result = getOrderBidsByItem(itemId, null, 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    @Test
    fun `get best sells - no currencies found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { orderService.getSellCurrencies(itemId.contract, itemId.tokenId.toString()) } returns emptyList()
        val result = getSellOrdersByItem(itemId, null, 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    @Test
    fun `get best bids - completed not requested`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethOrder = randomEthLegacyOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)

        coEvery {
            orderService.getBidCurrencies(ethItemId.contract, ethItemId.tokenId.toString())
        } returns listOf(
            EthErc20AssetTypeDto(
                ContractAddressConverter.convert(
                    ethItemId.blockchain,
                    unionOrder.bidCurrencyId
                )
            )
        )

        val result = getOrderBidsByItem(ethItemId, "${unionOrder.bidCurrencyId}:COMPLETED", 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    private suspend fun getOrderBidsByItem(itemId: ItemIdDto, continuation: String?, size: Int): Slice<OrderDto> {
        return orderApiService.getOrderBidsByItem(
            blockchain = BlockchainDto.ETHEREUM,
            platform = PlatformDto.RARIBLE,
            contract = itemId.contract,
            tokenId = itemId.tokenId.toString(),
            maker = null,
            origin = null,
            status = emptyList(),
            start = null,
            end = null,
            continuation = continuation,
            size = size
        )
    }

    private suspend fun getSellOrdersByItem(itemId: ItemIdDto, continuation: String?, size: Int): Slice<OrderDto> {
        return orderApiService.getSellOrdersByItem(
            blockchain = BlockchainDto.ETHEREUM,
            platform = PlatformDto.RARIBLE,
            contract = itemId.contract,
            tokenId = itemId.tokenId.toString(),
            maker = null,
            origin = null,
            status = null,
            continuation = continuation,
            size = size
        )
    }

}