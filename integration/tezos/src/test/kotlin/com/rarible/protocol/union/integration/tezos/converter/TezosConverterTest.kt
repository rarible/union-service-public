package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.AssetDto
import com.rarible.protocol.tezos.dto.FA_1_2AssetTypeDto
import com.rarible.protocol.tezos.dto.FA_2AssetTypeDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.TezosFA12AssetTypeDto
import com.rarible.protocol.union.dto.TezosFA2AssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosConverterTest {

    @Test
    fun `asset`() {
        val asset = AssetDto(XTZAssetTypeDto(), randomBigDecimal().stripTrailingZeros())

        val converted = TezosConverter.convert(asset, BlockchainDto.TEZOS)

        assertThat(converted.value).isEqualTo(asset.value)
        assertThat(converted.type).isInstanceOf(TezosXTZAssetTypeDto::class.java)
    }

    @Test
    fun `asset type - xtz`() {
        val assetType = XTZAssetTypeDto()

        val converted = TezosConverter.convert(assetType, BlockchainDto.TEZOS)

        assertThat(converted).isInstanceOf(TezosXTZAssetTypeDto::class.java)
    }

    @Test
    fun `asset type - fa1_2`() {
        val assetType = FA_1_2AssetTypeDto(
            contract = randomString()
        )

        val converted = TezosConverter.convert(assetType, BlockchainDto.TEZOS) as TezosFA12AssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
    }

    @Test
    fun `asset type - fa2`() {
        val assetType = FA_2AssetTypeDto(
            contract = randomString(),
            tokenId = randomBigInt()
        )

        val converted = TezosConverter.convert(assetType, BlockchainDto.TEZOS) as TezosFA2AssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

}