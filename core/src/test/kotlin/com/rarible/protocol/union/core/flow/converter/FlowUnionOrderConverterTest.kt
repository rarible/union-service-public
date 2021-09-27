package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOrderV1Dto
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class FlowUnionOrderConverterTest {

    @Test
    fun `order V1`() {
        val dto = randomFlowV1OrderDto()

        val converted = FlowUnionOrderConverter.convert(dto, BlockchainDto.FLOW) as FlowOrderV1Dto

        assertThat(converted.id.value).isEqualTo(dto.id.toString())
        assertThat(converted.startedAt).isNull()
        assertThat(converted.endedAt).isNull()
        assertThat(converted.makeStock).isEqualTo(dto.makeStock)
        assertThat(converted.fill).isEqualTo(dto.fill.toBigInteger())
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePriceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.taker?.value).isEqualTo(dto.taker)
        assertThat(converted.make.value).isEqualTo(dto.make.value.toBigInteger())
        assertThat(converted.make.type.contract.value).isEqualTo(dto.make.contract)
        assertThat(converted.take.value).isEqualTo(dto.take.value.toBigInteger())
        assertThat(converted.take.type.contract.value).isEqualTo(dto.take.contract)

         assertThat(converted.data.payouts[0].value).isEqualTo(dto.data.payouts[0].value.toBigInteger())
        assertThat(converted.data.payouts[0].account.value).isEqualTo(dto.data.payouts[0].account)

        assertThat(converted.data.originFees[0].value).isEqualTo(dto.data.originalFees[0].value.toBigInteger())
        assertThat(converted.data.originFees[0].account.value).isEqualTo(dto.data.originalFees[0].account)
    }
}
