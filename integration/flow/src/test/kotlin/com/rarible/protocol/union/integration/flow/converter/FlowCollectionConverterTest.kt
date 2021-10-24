package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.test.data.randomFlowCollectionDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowCollectionConverterTest {

    @Test
    fun `flow collection`() {
        val dto = randomFlowCollectionDto()

        val converted = FlowCollectionConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.owner?.value).isEqualTo(dto.owner)
    }

}