package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestOrderServiceTest {

    private val enrichmentOrderService: EnrichmentOrderService = mockk()

    private val currencyService: CurrencyService = mockk {
        coEvery { getCurrentRate(any(), any()) } returns CurrencyUsdRateDto(
            currencyId = "test",
            rate = BigDecimal.ONE,
            date = nowMillis()
        )
    }

    private val bestOrderService = BestOrderService(enrichmentOrderService, currencyService)

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentOrderService)
    }

    @Test
    fun `item best sell order - updated is alive, current is null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null, bestSellOrders = emptyMap())
        val updated = randomUnionSellOrderDto(itemId)

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        // If current best Order is missing, updated should be set as best Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId)
        val current = updated.copy(makePrice = randomBigDecimal().stripTrailingZeros())
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        // If current Order is the same as updated, updated should be set as best Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(
            makePrice = randomBigDecimal(3, 1)
        )
        val current = randomUnionSellOrderDto(itemId).copy(
            makePrice = updated.makePrice!! + (BigDecimal.ONE)
        )
        val item = randomShortItem(itemId)
            .copy(
                bestSellOrders = mapOf(current.sellCurrencyId to ShortOrderConverter.convert(current)),
                bestSellOrder = ShortOrderConverter.convert(current)
            )

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        // We have better sell Order, replacing current best Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current has preferred type`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(
            makePrice = randomBigDecimal(3, 1),
            platform = PlatformDto.CRYPTO_PUNKS
        )
        // Current has higher makePrice, but it has preferred type
        val current = randomUnionSellOrderDto(itemId).copy(
            makePrice = updated.makePrice!!.plus(BigDecimal.ONE)
        )
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(current))
    }

    @Test
    fun `item best sell order - updated is alive, updated has preferred type`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(
            makePrice = randomBigDecimal(3, 1)
        )
        // Current is better than updated, but updated has preferred type
        val current = randomUnionSellOrderDto(itemId).copy(
            makePrice = updated.makePrice!!.minus(BigDecimal.ONE),
            platform = PlatformDto.CRYPTO_PUNKS
        )
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, both orders doesn't have preferred type`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(
            makePrice = randomBigDecimal(3, 1),
            platform = PlatformDto.OPEN_SEA
        )
        val current = randomUnionSellOrderDto(itemId).copy(
            makePrice = updated.makePrice!!.plus(BigDecimal.ONE),
            platform = PlatformDto.CRYPTO_PUNKS
        )
        val item = randomShortItem(itemId)
            .copy(
                bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
                bestSellOrder = ShortOrderConverter.convert(current)
            )

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        // Updated Order has better price, should be set as best Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current is still the best`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(
            makePrice = randomBigDecimal(3, 1)
        )
        val current = randomUnionSellOrderDto(itemId).copy(
            makePrice = updated.makePrice!!.minus(BigDecimal.ONE)
        )
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )
        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)
        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(current))
    }

    @Test
    fun `item best sell order - updated is dead, current is null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null)
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.CANCELLED)

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        assertThat(updatedShortItem.bestSellOrder).isNull()
    }

    @Test
    fun `item best sell order - updated is dead, current with preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId)
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.FILLED)

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        assertThat(updatedShortItem.bestSellOrder).isEqualTo(item.bestSellOrder)
    }

    @Test
    fun `item best sell order - updated is dead, current without preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val current = randomUnionSellOrderDto(itemId).copy(platform = PlatformDto.OPEN_SEA)
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.INACTIVE)
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated)

        assertThat(updatedShortItem.bestSellOrder).isEqualTo(item.bestSellOrder)
    }

    @Test
    fun `item best bid order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionBidOrderDto(itemId).copy(takePrice = randomBigDecimal(3, 1))
        val current = randomUnionBidOrderDto(itemId).copy(takePrice = updated.takePrice!!.minus(BigDecimal.ONE))
        val item = ShortItemConverter.convert(randomUnionItem(itemId))
            .copy(
                bestBidOrders = mapOf(updated.bidCurrencyId to ShortOrderConverter.convert(current)),
                bestBidOrder = ShortOrderConverter.convert(current)
            )

        val updatedShortItem = bestOrderService.updateBestBidOrder(item, updated)

        // We have better bid Order, replacing current best Order
        assertThat(updatedShortItem.bestBidOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `ownership best sell order - updated is dead, current is the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.FILLED)
        val currencyId = updated.sellCurrencyId
        val current =
            updated.copy(makePriceUsd = randomBigDecimal().stripTrailingZeros(), status = OrderStatusDto.ACTIVE)
        val fetched = randomUnionSellOrderDto(itemId)

        val ownership = randomShortOwnership(itemId).copy(
            bestSellOrders = mapOf(currencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )
        val ownershipId = ownership.id

        coEvery { enrichmentOrderService.getBestSell(ownershipId, any()) } returns fetched

        val updatedShortItem = bestOrderService.updateBestSellOrder(ownership, updated)

        // Dead best Order should be replaced by fetched Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(fetched))
    }
}
