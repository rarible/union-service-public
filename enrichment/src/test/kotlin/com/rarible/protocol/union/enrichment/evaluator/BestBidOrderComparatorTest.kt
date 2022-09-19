package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestBidOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1))
        val updated = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(2))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(2))
        val updated = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomUnionSellOrderDto().copy(takePrice = null)
        val updated = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomUnionSellOrderDto().copy(takePrice = null)
        val updated = randomUnionSellOrderDto().copy(takePrice = null)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated has same price as current`() {
        val current = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1))
        val updated = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        // Current best order should not be changed
        assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated has same price as current and from RARIBLE`() {
        val current = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1), platform = PlatformDto.X2Y2)
        val updated = randomUnionSellOrderDto().copy(takePrice = BigDecimal.valueOf(1), platform = PlatformDto.RARIBLE)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        // Current best order should be changed
        assertThat(result).isEqualTo(shortUpdated)
    }
}
