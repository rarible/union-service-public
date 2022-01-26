package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemValidatorTest {

    @Test
    fun `valid item`() {
        val empty = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
        val bothValid = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(
                bestSellOrder = randomUnionSellOrderDto(),
                bestBidOrder = randomUnionBidOrderDto()
            )

        val sellValid = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(bestSellOrder = randomUnionSellOrderDto())

        val bidValid = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(bestBidOrder = randomUnionBidOrderDto())

        assertThat(ItemValidator.isValid(null)).isTrue()
        assertThat(ItemValidator.isValid(empty)).isTrue()
        assertThat(ItemValidator.isValid(bothValid)).isTrue()
        assertThat(ItemValidator.isValid(sellValid)).isTrue()
        assertThat(ItemValidator.isValid(bidValid)).isTrue()
    }

    @Test
    fun `invalid item`() {
        val bothInvalid = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(
                bestSellOrder = randomUnionSellOrderDto().copy(taker = randomUnionAddress()),
                bestBidOrder = randomUnionBidOrderDto().copy(status = OrderStatusDto.INACTIVE)
            )

        val bidInvalid = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(
                bestSellOrder = randomUnionSellOrderDto(),
                bestBidOrder = randomUnionBidOrderDto().copy(status = OrderStatusDto.FILLED)
            )

        val sellInvalid = EnrichedItemConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(bestSellOrder = randomUnionSellOrderDto().copy(status = OrderStatusDto.CANCELLED))

        assertThat(ItemValidator.isValid(bothInvalid)).isFalse()
        assertThat(ItemValidator.isValid(bidInvalid)).isFalse()
        assertThat(ItemValidator.isValid(sellInvalid)).isFalse()
    }

}