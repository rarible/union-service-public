package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.protocol.union.core.model.UnionTezosFTAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosMTAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosNFTAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosXTZAssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TezosConverterTest {

    @Test
    fun `asset`() {
        val asset = Asset(Asset.XTZ(), randomBigDecimal())

        val converted = DipDupConverter.convert(asset, BlockchainDto.TEZOS)

        assertThat(converted.value).isEqualTo(asset.assetValue)
        assertThat(converted.type).isInstanceOf(UnionTezosXTZAssetTypeDto::class.java)
    }

    @Test
    fun `asset type - xtz`() {
        val assetType = Asset.XTZ()

        val converted = DipDupConverter.convert(assetType, BlockchainDto.TEZOS)

        assertThat(converted).isInstanceOf(UnionTezosXTZAssetTypeDto::class.java)
    }

    @Test
    fun `asset type - ft`() {
        val assetType = Asset.FT(
            contract = randomString(),
            tokenId = BigInteger.ZERO
        )

        val converted = DipDupConverter.convert(assetType, BlockchainDto.TEZOS) as UnionTezosFTAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
    }

    @Test
    fun `asset type - nft`() {
        val assetType = Asset.NFT(
            contract = randomString(),
            tokenId = randomBigInt()
        )

        val converted = DipDupConverter.convert(assetType, BlockchainDto.TEZOS) as UnionTezosNFTAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

    @Test
    fun `asset type - mt`() {
        val assetType = Asset.MT(
            contract = randomString(),
            tokenId = randomBigInt()
        )

        val converted = DipDupConverter.convert(assetType, BlockchainDto.TEZOS) as UnionTezosMTAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract)
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

}
