package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAssetTypeDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowUnionOrderConverterTest {

    @Test
    fun `order V1`() {
        val dto = randomFlowV1OrderDto()

        val converted = FlowUnionOrderConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id.toString())
        assertThat(converted.startedAt).isNull()
        assertThat(converted.endedAt).isNull()
        // assertThat(converted.makeStock).isEqualTo(dto.makeStock) TODO что-то с версией не так flow-protocol-api
        assertThat(converted.fill).isEqualTo(dto.fill.toBigInteger())
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        // assertThat(converted.makePriceUsd).isEqualTo(dto.priceUsd) TODO что-то с версией не так flow-protocol-api
        // assertThat(converted.takePriceUsd).isEqualTo(dto.priceUsd) TODO что-то с версией не так flow-protocol-api
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.taker?.value).isEqualTo(dto.taker)
        assertThat(converted.make.value).isEqualTo(dto.make.value.toBigInteger())
        val makeType = converted.make.type as FlowAssetTypeDto
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        assertThat(converted.take.value).isEqualTo(dto.take!!.value.toBigInteger())
        val takeType = converted.take.type as FlowAssetTypeDto
        assertThat(takeType.contract.value).isEqualTo(dto.take!!.contract)

        val data = converted.data as FlowOrderDataV1Dto
        assertThat(data.payouts[0].value).isEqualTo(dto.data.payouts[0].value.toBigInteger())
        assertThat(data.payouts[0].account.value).isEqualTo(dto.data.payouts[0].account)

        assertThat(data.originFees[0].value).isEqualTo(dto.data.originalFees[0].value.toBigInteger())
        assertThat(data.originFees[0].account.value).isEqualTo(dto.data.originalFees[0].account)
    }
}
