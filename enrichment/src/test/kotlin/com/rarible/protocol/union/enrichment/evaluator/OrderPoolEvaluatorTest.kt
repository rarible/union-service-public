package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderPoolEvaluatorTest {

    @Test
    fun `order added`() {
        val order = randomUnionSellOrder()
        val shortOrder = ShortOrderConverter.convert(order)

        val item = randomShortItem()

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, order, PoolItemAction.INCLUDED)

        assertThat(updated.poolSellOrders).hasSize(1)
        assertThat(updated.poolSellOrders[0].order).isEqualTo(shortOrder)
    }

    @Test
    fun `order updated`() {
        val order = randomUnionSellOrder()
        val shortOrder = ShortOrderConverter.convert(order)
        val currentShortOrder = shortOrder.copy(makePrice = randomBigDecimal())

        val item = randomShortItem().copy(
            poolSellOrders = listOf(ShortPoolOrder(order.getSellCurrencyId(), currentShortOrder))
        )

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, order, PoolItemAction.INCLUDED)

        // Should be replaced by updated version
        assertThat(updated.poolSellOrders).hasSize(1)
        assertThat(updated.poolSellOrders[0].order).isEqualTo(shortOrder)
    }

    @Test
    fun `order removed`() {
        val order = randomUnionSellOrder()
        val shortOrder = ShortOrderConverter.convert(order)

        val item = randomShortItem().copy(
            poolSellOrders = listOf(ShortPoolOrder(order.getSellCurrencyId(), shortOrder))
        )

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, order, PoolItemAction.EXCLUDED)

        assertThat(updated.poolSellOrders).hasSize(0)
    }

    @Test
    fun `order ignored`() {
        val order = randomUnionSellOrder()
        val shortOrder = ShortOrderConverter.convert(order)

        val item = randomShortItem().copy(
            poolSellOrders = listOf(ShortPoolOrder(order.getSellCurrencyId(), shortOrder))
        )

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, randomUnionSellOrder(), PoolItemAction.EXCLUDED)

        // Non-existing order is removed, no changes expected
        assertThat(updated).isEqualTo(item)
    }

}