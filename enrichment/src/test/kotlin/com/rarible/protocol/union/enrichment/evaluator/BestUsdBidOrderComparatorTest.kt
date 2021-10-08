package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.test.data.randomUnionOrderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestUsdBidOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomUnionOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val updated = randomUnionOrderDto().copy(takePriceUsd = BigDecimal.valueOf(2))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestUsdBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomUnionOrderDto().copy(takePriceUsd = BigDecimal.valueOf(2))
        val updated = randomUnionOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestUsdBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomUnionOrderDto().copy(takePriceUsd = null)
        val updated = randomUnionOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestUsdBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomUnionOrderDto().copy(takePriceUsd = null)
        val updated = randomUnionOrderDto().copy(takePriceUsd = null)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestUsdBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

}
