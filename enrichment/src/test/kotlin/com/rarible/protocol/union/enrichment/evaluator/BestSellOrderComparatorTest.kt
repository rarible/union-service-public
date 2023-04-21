package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestSellOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(2))
        val updated = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1))
        val updated = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(2))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomUnionSellOrder().copy(makePrice = null)
        val updated = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomUnionSellOrder().copy(makePrice = null)
        val updated = randomUnionSellOrder().copy(makePrice = null)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated has same price as current`() {
        val current = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1))
        val updated = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        // Current best order should not be changed
        assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `update has same price as current and update from RARIBLE`() {
        val current = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1), platform = PlatformDto.X2Y2)
        val updated = randomUnionSellOrder().copy(makePrice = BigDecimal.valueOf(1), platform = PlatformDto.RARIBLE)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)

        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)

        assertThat(result).isEqualTo(shortUpdated)
    }
}
