package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomUnionItem
import com.rarible.protocol.union.test.data.randomUnionOrderDto
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
        coEvery { getCurrencyRate(any(), any(), any()) } returns CurrencyUsdRateDto(
            currencyId = "test",
            rate = BigDecimal.ONE,
            date = nowMillis()
        )
    }

    private val bestUsdOrderReducer = BestOrderReducer(currencyService)
    private val bestOrderService = BestOrderService(enrichmentOrderService, bestUsdOrderReducer)

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentOrderService)
    }

    @Test
    fun `item best sell order - updated is alive, current null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null, bestSellOrders = emptyMap())
        val updated = randomUnionOrderDto(itemId)

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        // If current best Order is missing, updated should be set as best Order
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(updated).clearState())
    }

    @Test
    fun `item best sell order - updated is alive, current the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId)
        val current = updated.copy(makePriceUsd = randomBigDecimal())
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        // If current Order is the same as updated, updated should be set as best Order
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(updated).clearState())
    }

    @Test
    fun `item best sell order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        val current = randomUnionOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE))
        val item = randomShortItem(itemId)
            .copy(bestSellOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        // We have better sell Order, replacing current best Order
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(updated).clearState())
    }

    @Test
    fun `item best sell order - updated is alive, current has preferred type`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(
            makePriceUsd = randomBigDecimal(3, 1),
            platform = PlatformDto.CRYPTO_PUNKS
        )
        // Current has higher takePrice, but it has preferred type
        val current = randomUnionOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE))
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(current).clearState())
    }

    @Test
    fun `item best sell order - updated is alive, updated has preferred type`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        // Current is better than updated, but updated has preferred type
        val current = randomUnionOrderDto(itemId).copy(
            makePriceUsd = updated.makePriceUsd!!.minus(BigDecimal.ONE),
            platform = PlatformDto.OPEN_SEA
        )
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(updated).clearState())
    }

    @Test
    fun `item best sell order - updated is alive, both orders doesn't have preferred type`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(
            makePriceUsd = randomBigDecimal(3, 1),
            platform = PlatformDto.OPEN_SEA
        )
        val current = randomUnionOrderDto(itemId).copy(
            makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE),
            platform = PlatformDto.CRYPTO_PUNKS
        )
        val item = randomShortItem(itemId)
            .copy(bestSellOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        // Updated Order has better price, should be set as best Order
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(updated).clearState())
    }

    @Test
    fun `item best sell order - updated is alive, current is still the best`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(makePrice = randomBigDecimal(3, 1))
        val current = randomUnionOrderDto(itemId).copy(makePrice = updated.makePrice!!.minus(BigDecimal.ONE))
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )
        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)
        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(current).clearState())
    }

    @Test
    fun `item best sell order - updated is dead, current is null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null)
        val updated = randomUnionOrderDto(itemId).copy(cancelled = true)

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        assertThat(updatedShortItem.bestSellOrder).isNull()
    }

    @Test
    fun `item best sell order - updated is dead, current with preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId)
        val updated = randomUnionOrderDto(itemId).copy(cancelled = true)

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(item.bestSellOrder?.clearState())
    }

    @Test
    fun `item best sell order - updated is dead, current without preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val current = randomUnionOrderDto(itemId).copy(platform = PlatformDto.OPEN_SEA)
        val updated = randomUnionOrderDto(itemId).copy(makeStock = BigDecimal.ZERO)
        val item = randomShortItem(itemId).copy(
            bestSellOrders = mapOf(updated.sellCurrencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )

        val updatedShortItem = bestOrderService.getBestSellOrder(item, updated)

        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(item.bestSellOrder?.clearState())
    }

    @Test
    fun `item best bid order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(takePriceUsd = randomBigDecimal(3, 1))
        val current = randomUnionOrderDto(itemId).copy(takePriceUsd = updated.takePriceUsd!!.minus(BigDecimal.ONE))
        val item = ShortItemConverter.convert(randomUnionItem(itemId))
            .copy(bestBidOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.getBestBidOrder(item, updated)

        // We have better bid Order, replacing current best Order
        assertThat(updatedShortItem.bestBidOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(updated).clearState())
    }

    @Test
    fun `ownership best sell order - updated is dead, current is the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(cancelled = true)
        val currencyId = updated.sellCurrencyId
        val current = updated.copy(makePriceUsd = randomBigDecimal(), cancelled = false)
        val fetched = randomUnionOrderDto(itemId)

        val ownership = randomShortOwnership(itemId).copy(
            bestSellOrders = mapOf(currencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )
        val ownershipId = ownership.id

        coEvery { enrichmentOrderService.getBestSell(ownershipId, any()) } returns fetched

        val updatedShortItem = bestOrderService.getBestSellOrder(ownership, updated)

        // Dead best Order should be replaced by fetched Order
        assertThat(updatedShortItem.bestSellOrder?.clearState()).isEqualTo(ShortOrderConverter.convert(fetched).clearState())
    }
}
