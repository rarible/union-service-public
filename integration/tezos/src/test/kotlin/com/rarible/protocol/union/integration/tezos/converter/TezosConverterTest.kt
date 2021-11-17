package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.AssetDto
import com.rarible.protocol.tezos.dto.FTAssetTypeDto
import com.rarible.protocol.tezos.dto.MTAssetTypeDto
import com.rarible.protocol.tezos.dto.NFTAssetTypeDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosConverterTest {

    @Test
    fun `asset`() {
        val asset = AssetDto(XTZAssetTypeDto(), randomBigDecimal())

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
    fun `asset type - ft`() {
        val assetType = FTAssetTypeDto(
            contract = randomString()
        )

        val converted = TezosConverter.convert(assetType, BlockchainDto.TEZOS) as TezosFTAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
    }

    @Test
    fun `asset type - nft`() {
        val assetType = NFTAssetTypeDto(
            contract = randomString(),
            tokenId = randomBigInt()
        )

        val converted = TezosConverter.convert(assetType, BlockchainDto.TEZOS) as TezosNFTAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

    @Test
    fun `asset type - mt`() {
        val assetType = MTAssetTypeDto(
            contract = randomString(),
            tokenId = randomBigInt()
        )

        val converted = TezosConverter.convert(assetType, BlockchainDto.TEZOS) as TezosMTAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

}