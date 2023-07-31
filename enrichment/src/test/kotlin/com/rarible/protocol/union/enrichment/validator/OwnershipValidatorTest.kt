package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.converter.OwnershipDtoConverter
import com.rarible.protocol.union.enrichment.test.data.randomSellOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OwnershipValidatorTest {

    @Test
    fun `valid item`() {
        val empty = OwnershipDtoConverter.convert(randomUnionOwnership(randomEthOwnershipId()))

        val sellValid = OwnershipDtoConverter.convert(randomUnionOwnership(randomEthOwnershipId()))
            .copy(bestSellOrder = randomSellOrderDto())

        assertThat(OwnershipValidator.isValid(null)).isTrue()
        assertThat(OwnershipValidator.isValid(empty)).isTrue()
        assertThat(OwnershipValidator.isValid(sellValid)).isTrue()
    }

    @Test
    fun `invalid item`() {
        val sellInvalid = OwnershipDtoConverter.convert(randomUnionOwnership(randomEthOwnershipId()))
            .copy(bestSellOrder = randomSellOrderDto().copy(status = OrderStatusDto.CANCELLED))

        assertThat(OwnershipValidator.isValid(sellInvalid)).isFalse()
    }
}
