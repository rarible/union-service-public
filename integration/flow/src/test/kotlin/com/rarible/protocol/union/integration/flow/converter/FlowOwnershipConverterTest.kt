package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOwnershipDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class FlowOwnershipConverterTest {

    @Test
    fun `flow ownership`() {
        val dto = randomFlowNftOwnershipDto()

        val converted = FlowOwnershipConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isNull() // TODO FLOW update later
        assertThat(converted.value).isEqualTo(BigInteger.ONE)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract)

        assertThat(converted.id.owner.value).isEqualTo(dto.owner)
        assertThat(converted.creators[0].value).isEqualTo(FlowConverter.toBasePoints(dto.creators[0].value))
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
    }

}
