package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
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

    private val enrichmentItemService: EnrichmentItemService = mockk()
    private val router: BlockchainRouter<OrderService> = mockk()
    private val orderService: OrderService = mockk()
    private val ethOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val orderApiService = OrderApiService(
        router,
        enrichmentItemService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentItemService, router, orderService)
        every { router.getService(BlockchainDto.ETHEREUM) } returns orderService
    }

    @Test
    fun `get best bids - no item found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { enrichmentItemService.get(ShortItemId(itemId)) } returns null
        val result = getOrderBidsByItem(itemId, null, 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    @Test
    fun `get best sells - no item found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { enrichmentItemService.get(ShortItemId(itemId)) } returns null
        val result = getSellOrdersByItem(itemId, null, 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    @Test
    fun `get best bids - completed not requested`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomUnionItem(ethItemId)

        val order = randomEthLegacyOrderDto(ethItemId)
        val unionOrder = ethOrderConverter.convert(order, ethItemId.blockchain)
        val shortOrder = ShortOrderConverter.convert(unionOrder)
        val shortItem = ShortItemConverter.convert(ethItem).copy(
            bestBidOrder = shortOrder,
            bestBidOrders = mapOf(unionOrder.bidCurrencyId to shortOrder)
        )

        coEvery { enrichmentItemService.get(ShortItemId(ethItemId)) } returns shortItem
        val result = getOrderBidsByItem(ethItemId, "${unionOrder.bidCurrencyId}:COMPLETED", 10)

        assertThat(result.continuation).isNull()
        assertThat(result.entities).hasSize(0)
    }

    private suspend fun getOrderBidsByItem(itemId: ItemIdDto, continuation: String?, size: Int): Slice<OrderDto> {
        return orderApiService.getOrderBidsByItem(
            blockchain = BlockchainDto.ETHEREUM,
            platform = PlatformDto.RARIBLE,
            contract = itemId.token.value,
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
            contract = itemId.token.value,
            tokenId = itemId.tokenId.toString(),
            maker = null,
            origin = null,
            status = null,
            continuation = continuation,
            size = size
        )
    }

}