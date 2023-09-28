package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionSetBaseUriEventDto
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthEventTimeMarks
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.util.UUID

class EthCollectionConverterTest {

    @Test
    fun `eth collection erc721`() {
        val dto = randomEthCollectionDto()
            .copy(features = NftCollectionDto.Features.values().asList())
            .copy(type = NftCollectionDto.Type.ERC721)
            .copy(status = NftCollectionDto.Status.PENDING)

        val converted = EthCollectionConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.id.value).isEqualTo(dto.id.prefixed())
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.type.name).isEqualTo(dto.type.name)
        assertThat(converted.status).isEqualTo(UnionCollection.Status.PENDING)
        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
        assertThat(converted.features.map { it.name }).isEqualTo(dto.features.map { it.name })
        assertThat(converted.scam).isEqualTo(dto.scam)
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
    fun `setBaseUri event`() {
        val event = NftCollectionSetBaseUriEventDto(
            eventId = UUID.randomUUID().toString(),
            id = Address.ONE(),
            eventTimeMarks = randomEthEventTimeMarks(),
        )

        val result = EthCollectionConverter.convert(event, BlockchainDto.ETHEREUM)

        assertThat(result.collectionId.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.collectionId.value).isEqualTo(Address.ONE().toString())
        assertThat(result.eventTimeMarks).isNotNull
    }

    /*
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
        val contentImage = meta.content.first()

        assertThat(contentImage).isExactlyInstanceOf(UnionMetaContent::class.java)

        val expected = dto.meta!!.content[0] as ImageContentDto
        assertThat(contentImage.url).isEqualTo(expected.url)
        assertThat(contentImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)

        val properties = contentImage.properties!!
        assertThat(properties).isExactlyInstanceOf(UnionImageProperties::class.java)
        properties as UnionImageProperties
        assertThat(properties.mimeType).isEqualTo(expected.mimeType)
        assertThat(properties.width).isEqualTo(expected.width)
        assertThat(properties.height).isEqualTo(expected.height)
        assertThat(properties.size).isEqualTo(expected.size)
    }
    */
}
