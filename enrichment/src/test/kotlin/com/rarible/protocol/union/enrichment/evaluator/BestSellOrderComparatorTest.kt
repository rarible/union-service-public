package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestSellOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomUnionSellOrderDto().copy(makePrice = BigDecimal.valueOf(2))
        val updated = randomUnionSellOrderDto().copy(makePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomUnionSellOrderDto().copy(makePrice = BigDecimal.valueOf(1))
        val updated = randomUnionSellOrderDto().copy(makePrice = BigDecimal.valueOf(2))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomUnionSellOrderDto().copy(makePrice = null)
        val updated = randomUnionSellOrderDto().copy(makePrice = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomUnionSellOrderDto().copy(makePrice = null)
        val updated = randomUnionSellOrderDto().copy(makePrice = null)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortUpdated)
    }
}
