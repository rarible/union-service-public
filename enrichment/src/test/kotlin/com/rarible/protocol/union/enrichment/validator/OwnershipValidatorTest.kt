package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnershipDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OwnershipValidatorTest {

    @Test
    fun `valid item`() {
        val empty = EnrichedOwnershipConverter.convert(randomUnionOwnershipDto(randomEthOwnershipId()))

        val sellValid = EnrichedOwnershipConverter.convert(randomUnionOwnershipDto(randomEthOwnershipId()))
            .copy(bestSellOrder = randomUnionSellOrderDto())

        assertThat(OwnershipValidator.isValid(null)).isTrue()
        assertThat(OwnershipValidator.isValid(empty)).isTrue()
        assertThat(OwnershipValidator.isValid(sellValid)).isTrue()
    }

    @Test
    fun `invalid item`() {
        val sellInvalid = EnrichedOwnershipConverter.convert(randomUnionOwnershipDto(randomEthOwnershipId()))
            .copy(bestSellOrder = randomUnionSellOrderDto().copy(status = OrderStatusDto.CANCELLED))

        assertThat(OwnershipValidator.isValid(sellInvalid)).isFalse()
    }
}