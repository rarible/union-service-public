package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthCollectionConverterTest {

    @Test
    fun `eth collection erc721`() {
        val dto = randomEthCollectionDto()
            .copy(features = NftCollectionDto.Features.values().asList())
            .copy(type = NftCollectionDto.Type.ERC721)

        val converted = EthCollectionConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.id.value).isEqualTo(dto.id.prefixed())
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.type.name).isEqualTo(dto.type.name)
        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
        assertThat(converted.features.map { it.name }).isEqualTo(dto.features.map { it.name })
    }

    @Test
    fun `eth collection convert - erc1155`() {
        val dto = randomEthCollectionDto()
            .copy(type = NftCollectionDto.Type.ERC1155)

        val converted = EthCollectionConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.type.name).isEqualTo(dto.type.name)
    }

    @Test
    fun `eth collection crypto punks`() {
        val dto = randomEthCollectionDto()
            .copy(type = NftCollectionDto.Type.CRYPTO_PUNKS)

        val converted = EthCollectionConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.type.name).isEqualTo(dto.type.name)
    }

    @Test
    fun `eth collection without owner`() {
        val dto = randomEthCollectionDto()
            .copy(owner = null)

        val converted = EthCollectionConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.owner).isNull()
    }

    @Test
    fun `eth collection - convert meta`() {
        val dto = randomEthCollectionDto()
        val originalMeta = dto.meta!!

        val actual = EthCollectionConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(actual.meta).isNotNull()
        val meta = actual.meta!!
        assertThat(meta.name).isEqualTo(originalMeta.name)
        assertThat(meta.description).isEqualTo(originalMeta.description)
        assertThat(meta.content).hasSize(1)
        val contentImage = meta.content!!.first()

        assertThat(contentImage).isExactlyInstanceOf(UnionMetaContent::class.java)
        val originalUrl = originalMeta.image!!.url.values.first()
        val originalMediaMeta = originalMeta.image!!.meta.values.first()
        assertThat(contentImage.url).isEqualTo(originalUrl)
        assertThat(contentImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)

        val properties = contentImage.properties!!
        assertThat(properties).isExactlyInstanceOf(UnionImageProperties::class.java)
        properties as UnionImageProperties
        assertThat(properties.mimeType).isEqualTo(originalMediaMeta.type)
        assertThat(properties.width).isEqualTo(originalMediaMeta.width)
        assertThat(properties.height).isEqualTo(originalMediaMeta.height)
        assertThat(properties.size).isNull()
    }
}