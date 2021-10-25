package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowOrderConverterTest {

    private val flowOrderConverter = FlowOrderConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `order V1`() = runBlocking<Unit> {
        val dto = randomFlowV1OrderDto()

        val converted = flowOrderConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id.toString())
        assertThat(converted.platform).isEqualTo(PlatformDto.RARIBLE)
        assertThat(converted.startedAt).isNull()
        assertThat(converted.endedAt).isNull()
        // TODO makeStock is needed to be BigDecimal on the flow client side
        assertThat(converted.makeStock).isEqualTo(dto.makeStock.toBigDecimal())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isNull()
        assertThat(converted.takePrice).isEqualTo(dto.take.value / dto.make.value)
        assertThat(converted.makePriceUsd).isNull()
        assertThat(converted.takePriceUsd).isEqualTo(dto.take.value / dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.taker?.value).isEqualTo(dto.taker)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        val makeType = converted.make.type as FlowAssetTypeNftDto
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        val takeType = converted.take.type as FlowAssetTypeFtDto
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)

        val data = converted.data as FlowOrderDataV1Dto
        assertThat(data.payouts[0].value).isEqualTo(dto.data.payouts[0].value.toBigInteger())
        assertThat(data.payouts[0].account.value).isEqualTo(dto.data.payouts[0].account)

        assertThat(data.originFees[0].value).isEqualTo(dto.data.originalFees[0].value.toBigInteger())
        assertThat(data.originFees[0].account.value).isEqualTo(dto.data.originalFees[0].account)
    }
}
