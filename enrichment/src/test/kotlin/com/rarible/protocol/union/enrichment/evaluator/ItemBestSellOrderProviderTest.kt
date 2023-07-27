package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import randomUnionOrder

class ItemBestSellOrderProviderTest {

    private val enrichmentOrderService: EnrichmentOrderService = mockk()

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentOrderService)
    }

    @Test
    fun `direct is the best - without pool orders`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val directOrder = randomUnionOrder()

        val shortItem = randomShortItem()
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns directOrder

        val result = provider.fetch(currencyId)

        assertThat(result).isEqualTo(directOrder)
    }

    @Test
    fun `direct not found - without pool orders`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val shortItem = randomShortItem()
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns null

        val result = provider.fetch(currencyId)

        assertThat(result).isNull()
    }

    @Test
    fun `direct is the best`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val directOrder = randomUnionOrder(makePrice = 1.toBigDecimal())

        val poolOrder = randomUnionOrder(makePrice = 2.toBigDecimal())
        val shortPoolOrder = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder))

        val shortItem = randomShortItem().copy(poolSellOrders = listOf(shortPoolOrder))
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns directOrder

        val result = provider.fetch(currencyId)

        assertThat(result).isEqualTo(directOrder)
    }

    @Test
    fun `direct is the best - pool order not found`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val directOrder = randomUnionOrder(makePrice = 2.toBigDecimal())

        val poolOrder = randomUnionOrder(makePrice = 1.toBigDecimal())
        val shortPoolOrder = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder))

        val shortItem = randomShortItem().copy(poolSellOrders = listOf(shortPoolOrder))
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns directOrder
        coEvery { enrichmentOrderService.getById(poolOrder.id) } returns null

        val result = provider.fetch(currencyId)

        assertThat(result).isEqualTo(directOrder)
    }

    @Test
    fun `direct is the best - origin specified`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val origin = randomAddressString()
        val directOrder = randomUnionOrder(makePrice = 2.toBigDecimal())

        val poolOrder = randomUnionOrder(makePrice = 1.toBigDecimal())
        val shortPoolOrder = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder))

        val shortItem = randomShortItem().copy(poolSellOrders = listOf(shortPoolOrder))
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService, origin)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, origin) } returns directOrder

        val result = provider.fetch(currencyId)

        // Even if pool order is better, direct order should be used since origin is specified in the request
        assertThat(result).isEqualTo(directOrder)
    }

    @Test
    fun `pool is the best`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val directOrder = randomUnionOrder(makePrice = 2.toBigDecimal())

        val poolOrder = randomUnionOrder(makePrice = 1.toBigDecimal())
        val shortPoolOrder = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder))

        val shortItem = randomShortItem().copy(poolSellOrders = listOf(shortPoolOrder))
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns directOrder
        coEvery { enrichmentOrderService.getById(poolOrder.id) } returns poolOrder

        val result = provider.fetch(currencyId)

        assertThat(result).isEqualTo(poolOrder)
    }

    @Test
    fun `pool is the best - several pool orders`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val poolOrder1 = randomUnionOrder(makePrice = 2.toBigDecimal())
        val shortPoolOrder1 = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder1))

        val poolOrder2 = randomUnionOrder(makePrice = 1.toBigDecimal())
        val shortPoolOrder2 = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder2))

        val shortItem = randomShortItem().copy(poolSellOrders = listOf(shortPoolOrder1, shortPoolOrder2))
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns null
        coEvery { enrichmentOrderService.getById(poolOrder2.id) } returns poolOrder2

        val result = provider.fetch(currencyId)

        // poolOrder2 has lower price
        assertThat(result).isEqualTo(poolOrder2)
    }

    @Test
    fun `pool is the best - several currencies`() = runBlocking<Unit> {
        val currencyId = randomAddressString()
        val poolOrder1 = randomUnionOrder(makePrice = 2.toBigDecimal())
        val shortPoolOrder1 = ShortPoolOrder(currencyId, ShortOrderConverter.convert(poolOrder1))

        // Price is better, but has different currency
        val poolOrder2 = randomUnionOrder(makePrice = 1.toBigDecimal())
        val shortPoolOrder2 = ShortPoolOrder(randomAddressString(), ShortOrderConverter.convert(poolOrder2))

        val shortItem = randomShortItem().copy(poolSellOrders = listOf(shortPoolOrder1, shortPoolOrder2))
        val provider = ItemBestSellOrderProvider(shortItem, enrichmentOrderService)

        coEvery { enrichmentOrderService.getBestSell(shortItem.id, currencyId, null) } returns null
        coEvery { enrichmentOrderService.getById(poolOrder1.id) } returns poolOrder1

        val result = provider.fetch(currencyId)

        assertThat(result).isEqualTo(poolOrder1)
    }
}
