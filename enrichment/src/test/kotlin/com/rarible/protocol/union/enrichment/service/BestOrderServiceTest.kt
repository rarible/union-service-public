package com.rarible.protocol.union.enrichment.service

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
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
    private val bestUsdOrderReducer: BestUsdOrderReducer = mockk()
    private val bestOrderService = BestOrderService(enrichmentOrderService, bestUsdOrderReducer)

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentOrderService)
    }

    @Test
    fun `item best sell order - updated is alive, current null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null)
        val updated = randomUnionOrderDto(itemId)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // If current best Order is missing, updated should be set as best Order
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId)
        val current = updated.copy(makePriceUsd = randomBigDecimal())
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // If current Order is the same as updated, updated should be set as best Order
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        val current = randomUnionOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE))
        val item = randomShortItem(itemId)
            .copy(bestSellOrder = ShortOrderConverter.convert(current))

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // We have better sell Order, replacing current best Order
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
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
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(current))
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

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
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

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Updated Order has better price, should be set as best Order
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current is still the best`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        val current = randomUnionOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.minus(BigDecimal.ONE))
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(current))
    }

    @Test
    fun `item best sell order - updated is dead, current is null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null)
        val updated = randomUnionOrderDto(itemId).copy(cancelled = true)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        assertThat(bestSellOrder).isNull()
    }

    @Test
    fun `item best sell order - updated is dead, current with preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId)
        val updated = randomUnionOrderDto(itemId).copy(cancelled = true)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        assertThat(bestSellOrder).isEqualTo(item.bestSellOrder)
    }

    @Test
    fun `item best sell order - updated is dead, current without preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val current = randomUnionOrderDto(itemId).copy(platform = PlatformDto.OPEN_SEA)
        val item = randomShortItem(itemId).copy(
            bestSellOrder = ShortOrderConverter.convert(current)
        )
        val updated = randomUnionOrderDto(itemId).copy(makeStock = BigDecimal.ZERO)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        assertThat(bestSellOrder).isEqualTo(item.bestSellOrder)
    }

    @Test
    fun `item best bid order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(takePriceUsd = randomBigDecimal(3, 1))
        val current = randomUnionOrderDto(itemId).copy(takePriceUsd = updated.takePriceUsd!!.minus(BigDecimal.ONE))
        val item = ShortItemConverter.convert(randomUnionItem(itemId))
            .copy(bestBidOrder = ShortOrderConverter.convert(current))

        val bestBidOrder = bestOrderService.getBestBidOrder(item, updated)

        // We have better bid Order, replacing current best Order
        assertThat(bestBidOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `ownership best sell order - updated is dead, current is the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionOrderDto(itemId).copy(cancelled = true)
        val current = updated.copy(makePriceUsd = randomBigDecimal(), cancelled = false)
        val fetched = randomUnionOrderDto(itemId)

        val ownership = randomShortOwnership(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))
        val ownershipId = ownership.id

        coEvery { enrichmentOrderService.getBestSell(ownershipId, any()) } returns fetched

        val bestSellOrder = bestOrderService.getBestSellOrder(ownership, updated)

        // Dead best Order should be replaced by fetched Order
        assertThat(bestSellOrder).isEqualTo(ShortOrderConverter.convert(fetched))
    }
}
