package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.test.data.randomFlowNftOwnershipDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowUnionOwnershipConverterTest {

    @Test
    fun `flow ownership`() {
        val dto = randomFlowNftOwnershipDto()

        val converted = FlowUnionOwnershipConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        // assertThat(converted.value).isEqualTo(dto.value) // TODO

        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner)
        // assertThat(converted.creators).isEqualTo(dto.creators) // TODO
    }

}