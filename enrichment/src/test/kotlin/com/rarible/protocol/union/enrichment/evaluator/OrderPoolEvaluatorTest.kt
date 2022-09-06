package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.solana.data.randomBigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderPoolEvaluatorTest {

    @Test
    fun `order added`() {
        val order = randomUnionSellOrderDto()
        val shortOrder = ShortOrderConverter.convert(order)

        val item = randomShortItem()

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, order, PoolItemAction.INCLUDED)

        assertThat(updated.poolSellOrders).hasSize(1)
        assertThat(updated.poolSellOrders[0].order).isEqualTo(shortOrder)
    }

    @Test
    fun `order updated`() {
        val order = randomUnionSellOrderDto()
        val shortOrder = ShortOrderConverter.convert(order)
        val currentShortOrder = shortOrder.copy(makePrice = randomBigDecimal())

        val item = randomShortItem().copy(
            poolSellOrders = listOf(ShortPoolOrder(order.sellCurrencyId, currentShortOrder))
        )

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, order, PoolItemAction.INCLUDED)

        // Should be replaced by updated version
        assertThat(updated.poolSellOrders).hasSize(1)
        assertThat(updated.poolSellOrders[0].order).isEqualTo(shortOrder)
    }

    @Test
    fun `order removed`() {
        val order = randomUnionSellOrderDto()
        val shortOrder = ShortOrderConverter.convert(order)

        val item = randomShortItem().copy(
            poolSellOrders = listOf(ShortPoolOrder(order.sellCurrencyId, shortOrder))
        )

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, order, PoolItemAction.EXCLUDED)

        assertThat(updated.poolSellOrders).hasSize(0)
    }

    @Test
    fun `order ignored`() {
        val order = randomUnionSellOrderDto()
        val shortOrder = ShortOrderConverter.convert(order)

        val item = randomShortItem().copy(
            poolSellOrders = listOf(ShortPoolOrder(order.sellCurrencyId, shortOrder))
        )

        val updated = OrderPoolEvaluator.updatePoolOrderSet(item, randomUnionSellOrderDto(), PoolItemAction.EXCLUDED)

        // Non-existing order is removed, no changes expected
        assertThat(updated).isEqualTo(item)
    }

}