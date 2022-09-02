package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.OriginOrders
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
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
            symbol = "test",
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

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

        // If current best Order is missing, updated should be set as best Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId)
        val current = updated.copy(makePrice = randomBigDecimal())
        val item = randomShortItem(itemId).copy(bestSellOrder = ShortOrderConverter.convert(current))

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

        // If current Order is the same as updated, updated should be set as best Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(
            makePrice = randomBigDecimal(3, 1),
            platform = PlatformDto.CRYPTO_PUNKS
        )
        val current = randomUnionSellOrderDto(itemId).copy(
            makePrice = updated.makePrice!! + (BigDecimal.ONE),
            platform = PlatformDto.RARIBLE
        )
        val item = randomShortItem(itemId)
            .copy(
                bestSellOrders = mapOf(current.sellCurrencyId to ShortOrderConverter.convert(current)),
                bestSellOrder = ShortOrderConverter.convert(current)
            )

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

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

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `item best sell order - updated is alive, ignore that updated has preferred type`() = runBlocking<Unit> {
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

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(current))
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

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

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
        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())
        // Current best Order is still better than updated
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(current))
    }

    @Test
    fun `item best sell order - updated is dead, current is null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(bestSellOrder = null)
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.CANCELLED)

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

        assertThat(updatedShortItem.bestSellOrder).isNull()
    }

    @Test
    fun `item best sell order - updated is dead, current with preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId)
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.FILLED)

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

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

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updated, emptyList())

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

        val updatedShortItem = bestOrderService.updateBestBidOrder(item, updated, emptyList())

        // We have better bid Order, replacing current best Order
        assertThat(updatedShortItem.bestBidOrder).isEqualTo(ShortOrderConverter.convert(updated))
    }

    @Test
    fun `ownership best sell order - updated is dead, current is the same`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val updated = randomUnionSellOrderDto(itemId).copy(status = OrderStatusDto.FILLED)
        val currencyId = updated.sellCurrencyId
        val current =
            updated.copy(makePriceUsd = randomBigDecimal(), status = OrderStatusDto.ACTIVE)
        val fetched = randomUnionSellOrderDto(itemId)

        val ownership = randomShortOwnership(itemId).copy(
            bestSellOrders = mapOf(currencyId to ShortOrderConverter.convert(current)),
            bestSellOrder = ShortOrderConverter.convert(current)
        )
        val ownershipId = ownership.id

        coEvery { enrichmentOrderService.getBestSell(ownershipId, any(), any()) } returns fetched

        val updatedShortItem = bestOrderService.updateBestSellOrder(ownership, updated, emptyList())

        // Dead best Order should be replaced by fetched Order
        assertThat(updatedShortItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(fetched))
    }

    // For origin orders there is no sense to test all the cases since same update mechanism used for them
    @Test
    fun `origin orders - best sell updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val origin = randomAddressString()
        val origins = listOf(origin)

        val updatedSell = randomUnionSellOrderDto(itemId = itemId, origins = origins).copy(makePrice = BigDecimal.ONE)
        val currentSell = randomUnionSellOrderDto(itemId = itemId, origins = origins).copy(makePrice = BigDecimal.TEN)
        val currentBid = randomUnionBidOrderDto()

        val shortCurrentSell = ShortOrderConverter.convert(currentSell)
        val shortUpdatedSell = ShortOrderConverter.convert(updatedSell)
        val shortCurrentBid = ShortOrderConverter.convert(currentBid)

        val current = OriginOrders(
            origin = origin,
            bestSellOrder = shortCurrentSell,
            bestSellOrders = mapOf(updatedSell.sellCurrencyId to shortCurrentSell),
            bestBidOrder = shortCurrentBid,
            bestBidOrders = mapOf(currentBid.bidCurrencyId to shortCurrentBid),
        )

        val item = randomShortItem(itemId).copy(originOrders = setOf(current))

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updatedSell, listOf(origin))

        assertThat(updatedShortItem.originOrders).hasSize(1)

        // Best sell should be updated for the origin, best bid should stay the same
        val originOrders = updatedShortItem.originOrders.toList()[0]
        assertThat(originOrders.bestSellOrder).isEqualTo(shortUpdatedSell)
        assertThat(originOrders.bestBidOrder).isEqualTo(shortCurrentBid)
    }

    @Test
    fun `origin orders - origin orders became empty`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val origin = randomAddressString()

        val currentBid = randomUnionBidOrderDto(itemId = itemId, origins = listOf(origin))
        val updatedBid = currentBid.copy(status = OrderStatusDto.FILLED)

        val shortCurrentBid = ShortOrderConverter.convert(currentBid)

        val current = OriginOrders(
            origin = origin,
            bestBidOrder = shortCurrentBid,
            bestBidOrders = mapOf(currentBid.bidCurrencyId to shortCurrentBid),
        )

        val item = randomShortItem(randomEthItemId()).copy(originOrders = setOf(current))
        coEvery { enrichmentOrderService.getBestBid(item.id, any(), origin) } returns null

        val updatedShortItem = bestOrderService.updateBestBidOrder(item, updatedBid, listOf(origin))

        // Origin orders record should be removed since there is no best orders
        assertThat(updatedShortItem.originOrders).hasSize(0)
    }

    @Test
    fun `origin orders - order without origin not updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val origin = randomAddressString()
        val origins = listOf(origin)

        val updatedSell = randomUnionSellOrderDto(itemId = itemId).copy(makePrice = BigDecimal.ONE)
        val currentSell = randomUnionSellOrderDto(itemId = itemId, origins = origins).copy(makePrice = BigDecimal.TEN)

        val shortCurrentSell = ShortOrderConverter.convert(currentSell)

        val current = OriginOrders(
            origin = origin,
            bestSellOrder = shortCurrentSell,
            bestSellOrders = mapOf(updatedSell.sellCurrencyId to shortCurrentSell)
        )

        val item = randomShortItem(itemId).copy(originOrders = setOf(current))

        val updatedShortItem = bestOrderService.updateBestSellOrder(item, updatedSell, listOf(origin))

        assertThat(updatedShortItem.originOrders).hasSize(1)

        // Best sell should stay the same, updated order doesn't relate to origin of current order
        val originOrders = updatedShortItem.originOrders.toList()[0]
        assertThat(originOrders.bestSellOrder).isEqualTo(shortCurrentSell)
    }
}
