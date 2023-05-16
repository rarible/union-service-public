package com.rarible.protocol.union.enrichment.service.query.order

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
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
    private val enrichmentItemService: EnrichmentItemService = mockk()
    private val orderApiService = OrderApiMergeService(
        router,
        enrichmentItemService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(router, orderService)
        every { router.getService(BlockchainDto.ETHEREUM) } returns orderService
    }

    @Test
    fun `get best bids - no currencies found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { orderService.getBidCurrencies(itemId.value, emptyList()) } returns emptyList()
        val result = getOrderBidsByItem(itemId, null, 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    @Test
    fun `get best sells - no currencies found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { orderService.getSellCurrencies(itemId.value, emptyList()) } returns emptyList()
        val result = getSellOrdersByItem(itemId, null, 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    @Test
    fun `get best bids - completed not requested`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethOrder = randomEthBidOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)

        coEvery {
            orderService.getBidCurrencies(ethItemId.value, emptyList())
        } returns listOf(
            UnionEthErc20AssetType(
                ContractAddressConverter.convert(
                    ethItemId.blockchain,
                    unionOrder.bidCurrencyId()
                )
            )
        )

        val result = getOrderBidsByItem(ethItemId, "${unionOrder.bidCurrencyId()}:COMPLETED", 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    private suspend fun getOrderBidsByItem(itemId: ItemIdDto, continuation: String?, size: Int): Slice<UnionOrder> {
        return orderApiService.getOrderBidsByItem(
            itemId = itemId,
            platform = PlatformDto.RARIBLE,
            maker = null,
            origin = null,
            status = emptyList(),
            currencies = null,
            start = null,
            end = null,
            continuation = continuation,
            size = size
        )
    }

    private suspend fun getSellOrdersByItem(itemId: ItemIdDto, continuation: String?, size: Int): Slice<UnionOrder> {
        return orderApiService.getSellOrdersByItem(
            itemId = itemId,
            platform = PlatformDto.RARIBLE,
            maker = null,
            origin = null,
            status = emptyList(),
            continuation = continuation,
            size = size
        )
    }

}