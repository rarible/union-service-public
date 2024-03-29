package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.converter.ItemDtoConverter
import com.rarible.protocol.union.enrichment.test.data.randomBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomSellOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemValidatorTest {

    @Test
    fun `valid item`() {
        val empty = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
        val bothValid = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(
                bestSellOrder = randomSellOrderDto(),
                bestBidOrder = randomBidOrderDto()
            )

        val sellValid = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(bestSellOrder = randomSellOrderDto())

        val bidValid = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(bestBidOrder = randomBidOrderDto())

        val item: ItemDto? = null
        assertThat(EntityValidator.isValid(item)).isTrue()
        assertThat(EntityValidator.isValid(empty)).isTrue()
        assertThat(EntityValidator.isValid(bothValid)).isTrue()
        assertThat(EntityValidator.isValid(sellValid)).isTrue()
        assertThat(EntityValidator.isValid(bidValid)).isTrue()
    }

    @Test
    fun `invalid item`() {
        val bothInvalid = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(
                bestSellOrder = randomSellOrderDto().copy(taker = randomUnionAddress()),
                bestBidOrder = randomBidOrderDto().copy(status = OrderStatusDto.INACTIVE)
            )

        val bidInvalid = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(
                bestSellOrder = randomSellOrderDto(),
                bestBidOrder = randomBidOrderDto().copy(status = OrderStatusDto.FILLED)
            )

        val sellInvalid = ItemDtoConverter.convert(randomUnionItem(randomEthItemId()))
            .copy(bestSellOrder = randomSellOrderDto().copy(status = OrderStatusDto.CANCELLED))

        assertThat(EntityValidator.isValid(bothInvalid)).isFalse()
        assertThat(EntityValidator.isValid(bidInvalid)).isFalse()
        assertThat(EntityValidator.isValid(sellInvalid)).isFalse()
    }
}
