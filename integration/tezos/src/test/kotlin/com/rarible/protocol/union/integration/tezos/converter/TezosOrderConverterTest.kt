package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.common.justOrEmpty
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosOrderDataLegacyDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosOrderConverterTest {

    private val tezosOrderConverter = DipDupOrderConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `tezos order`() = runBlocking<Unit> {
        val dto = randomTezosOrderDto()

        val converted = tezosOrderConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.platform).isEqualTo(PlatformDto.RARIBLE)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.make.type).isInstanceOf(TezosNFTAssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.assetValue)
        assertThat(converted.take.type).isInstanceOf(TezosXTZAssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.assetValue)
        assertThat(converted.salt).isEqualTo(dto.salt.toString())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.startedAt).isEqualTo(dto.startAt?.let { it.toInstant() })
        assertThat(converted.endedAt).isEqualTo(dto.endAt?.let { it.toInstant() })
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt.toInstant())
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt.toInstant())
        assertThat(converted.takePrice).isNull()
        assertThat(converted.makePrice).isEqualTo(converted.take.value.setScale(18) / converted.make.value.setScale(18))
        assertThat(converted.takePriceUsd).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(dto.take.assetValue.setScale(18) / dto.make.assetValue.setScale(18))

        assertThat(converted.data).isInstanceOf(TezosOrderDataRaribleV2DataV2Dto::class.java)
    }

}
