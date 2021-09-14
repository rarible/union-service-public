package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.test.data.randomFlowCollectionDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowUnionCollectionConverterTest {

    @Test
    fun `flow collection`() {
        val dto = randomFlowCollectionDto()

        val converted = FlowUnionCollectionConverter.convert(dto, FlowBlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
    }

}