package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.union.core.model.UnionEthErc1155AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc20AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc721AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc721LazyAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthEventTimeMarks
import com.rarible.protocol.union.integration.ethereum.data.randomEthPartDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class EthConverterTest {

    private val timeDelta = TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS)

    @Test
    fun platform() {
        assertThat(EthConverter.convert(null as PlatformDto?)).isEqualTo(null)
        assertThat(EthConverter.convert(PlatformDto.RARIBLE)).isEqualTo(com.rarible.protocol.dto.PlatformDto.RARIBLE)
        assertThat(EthConverter.convert(PlatformDto.OPEN_SEA)).isEqualTo(com.rarible.protocol.dto.PlatformDto.OPEN_SEA)
        assertThat(EthConverter.convert(PlatformDto.CRYPTO_PUNKS)).isEqualTo(com.rarible.protocol.dto.PlatformDto.CRYPTO_PUNKS)
        assertThat(EthConverter.convert(PlatformDto.LOOKSRARE)).isEqualTo(com.rarible.protocol.dto.PlatformDto.LOOKSRARE)
        assertThat(EthConverter.convert(PlatformDto.X2Y2)).isEqualTo(com.rarible.protocol.dto.PlatformDto.X2Y2)
        assertThat(EthConverter.convert(PlatformDto.SUDOSWAP)).isEqualTo(com.rarible.protocol.dto.PlatformDto.SUDOSWAP)
    }

    @Test
    fun `asset type - eth`() {
        val assetType = EthAssetTypeDto()

        val converted = EthConverter.convert(assetType, BlockchainDto.ETHEREUM)

        assertThat(converted).isInstanceOf(UnionEthEthereumAssetTypeDto::class.java)
    }

    @Test
    fun `asset type - erc20`() {
        val assetType = Erc20AssetTypeDto(
            contract = randomAddress()
        )

        val converted = EthConverter.convert(assetType, BlockchainDto.ETHEREUM) as UnionEthErc20AssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract.prefixed())
    }

    @Test
    fun `asset type - erc721`() {
        val assetType = Erc721AssetTypeDto(
            contract = randomAddress(),
            tokenId = randomBigInt()
        )

        val converted = EthConverter.convert(assetType, BlockchainDto.ETHEREUM) as UnionEthErc721AssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

    @Test
    fun `asset type - erc1155`() {
        val assetType = Erc1155AssetTypeDto(
            contract = randomAddress(),
            tokenId = randomBigInt()
        )

        val converted = EthConverter.convert(assetType, BlockchainDto.ETHEREUM) as UnionEthErc1155AssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
    }

    @Test
    fun `asset type - erc721 lazy`() {
        val creator = randomEthPartDto()
        val royalty = randomEthPartDto()
        val binary = randomBinary()
        val assetType = Erc721LazyAssetTypeDto(
            contract = randomAddress(),
            tokenId = randomBigInt(),
            uri = randomString(),
            creators = listOf(creator),
            royalties = listOf(royalty),
            signatures = listOf(binary)
        )

        val converted = EthConverter.convert(assetType, BlockchainDto.ETHEREUM) as UnionEthErc721LazyAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
        assertThat(converted.uri).isEqualTo(assetType.uri)
        assertThat(converted.creators[0].value).isEqualTo(assetType.creators[0].value)
        assertThat(converted.creators[0].account.value).isEqualTo(assetType.creators[0].account.prefixed())
        assertThat(converted.royalties[0].value).isEqualTo(assetType.royalties[0].value)
        assertThat(converted.royalties[0].account.value).isEqualTo(assetType.royalties[0].account.prefixed())
        assertThat(converted.signatures[0]).isEqualTo(assetType.signatures[0].prefixed())
    }

    @Test
    fun `asset type - erc1155 lazy`() {
        val creator = randomEthPartDto()
        val royalty = randomEthPartDto()
        val binary = randomBinary()
        val assetType = Erc1155LazyAssetTypeDto(
            contract = randomAddress(),
            tokenId = randomBigInt(),
            uri = randomString(),
            creators = listOf(creator),
            royalties = listOf(royalty),
            signatures = listOf(binary),
            supply = randomBigInt()
        )

        val converted = EthConverter.convert(assetType, BlockchainDto.ETHEREUM) as UnionEthErc1155LazyAssetTypeDto

        assertThat(converted.contract.value).isEqualTo(assetType.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(assetType.tokenId)
        assertThat(converted.uri).isEqualTo(assetType.uri)
        assertThat(converted.supply).isEqualTo(assetType.supply)
        assertThat(converted.creators[0].value).isEqualTo(assetType.creators[0].value)
        assertThat(converted.creators[0].account.value).isEqualTo(assetType.creators[0].account.prefixed())
        assertThat(converted.royalties[0].value).isEqualTo(assetType.royalties[0].value)
        assertThat(converted.royalties[0].account.value).isEqualTo(assetType.royalties[0].account.prefixed())
        assertThat(converted.signatures[0]).isEqualTo(assetType.signatures[0].prefixed())
    }

    @Test
    fun `time marks - ok`() {
        val marks = randomEthEventTimeMarks()
        val converted = EthConverter.convert(marks)!!

        assertThat(converted.source).isEqualTo(marks.source)
        assertThat(converted.marks).hasSize(marks.marks.size)
        assertThat(converted.marks[0].name).isEqualTo(marks.marks[0].name)
        assertThat(converted.marks[0].date).isEqualTo(marks.marks[0].date)
    }

    @Test
    fun `time marks - null`() {
        val marks: EventTimeMarksDto? = null
        assertThat(EthConverter.convert(marks)).isNull()
    }
}
