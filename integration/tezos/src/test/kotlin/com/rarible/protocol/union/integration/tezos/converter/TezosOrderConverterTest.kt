package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosOrderConverterTest {

    private val tezosOrderConverter = TezosOrderConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `tezos order`() = runBlocking<Unit> {
        val dto = randomTezosOrderDto()

        val converted = tezosOrderConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.hash)
        assertThat(converted.platform).isEqualTo(PlatformDto.RARIBLE)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!)
        assertThat(converted.make.type).isInstanceOf(TezosNFTAssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.take.type).isInstanceOf(TezosXTZAssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.salt).isEqualTo(dto.salt)
        assertThat(converted.signature).isEqualTo(dto.signature)
        assertThat(converted.fill).isEqualTo(dto.fill.toBigDecimal())
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start!!.toLong())
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end!!.toLong())
        assertThat(converted.makeStock).isEqualTo(dto.makeStock.toBigDecimal())
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isNull()
        assertThat(converted.takePrice).isEqualTo(converted.take.value / converted.make.value)
        assertThat(converted.makePriceUsd).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.takePriceUsd).isEqualTo(dto.take.value / dto.make.value)

        val data = converted.data as TezosOrderDataRaribleV2DataV1Dto
        assertThat(data.makerEdpk).isEqualTo(dto.makerEdpk)
        assertThat(data.takerEdpk).isEqualTo(dto.takerEdpk)
        assertThat(data.originFees[0].value).isEqualTo(dto.data.originFees[0].value)
        assertThat(data.originFees[0].account.value).isEqualTo(dto.data.originFees[0].account)
        assertThat(data.payouts[0].value).isEqualTo(dto.data.payouts[0].value)
        assertThat(data.payouts[0].account.value).isEqualTo(dto.data.payouts[0].account)
    }

}
