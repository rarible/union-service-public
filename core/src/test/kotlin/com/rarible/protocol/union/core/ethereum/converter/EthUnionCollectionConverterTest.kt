package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.test.data.randomEthCollectionDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthUnionCollectionConverterTest {

    @Test
    fun `eth collection erc721`() {
        val dto = randomEthCollectionDto()
            .copy(features = NftCollectionDto.Features.values().asList())
            .copy(type = NftCollectionDto.Type.ERC721)

        val converted = EthUnionCollectionConverter.convert(dto, EthBlockchainDto.ETHEREUM)

        assertThat(converted.id.value).isEqualTo(dto.id.prefixed())
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.type.name).isEqualTo(dto.type.name)
        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
        assertThat(converted.features.map { it.name }).isEqualTo(dto.features.map { it.name })
        assertThat(converted.supportsLazyMint).isEqualTo(dto.supportsLazyMint)
    }

    @Test
    fun `eth collection convert - erc1155`() {
        val dto = randomEthCollectionDto()
            .copy(type = NftCollectionDto.Type.ERC1155)

        val converted = EthUnionCollectionConverter.convert(dto, EthBlockchainDto.ETHEREUM)

        assertThat(converted.type.name).isEqualTo(dto.type.name)
    }

    @Test
    fun `eth collection crypto punks`() {
        val dto = randomEthCollectionDto()
            .copy(type = NftCollectionDto.Type.CRYPTO_PUNKS)

        val converted = EthUnionCollectionConverter.convert(dto, EthBlockchainDto.ETHEREUM)

        assertThat(converted.type.name).isEqualTo(dto.type.name)
    }

    @Test
    fun `eth collection without owner`() {
        val dto = randomEthCollectionDto()
            .copy(owner = null)

        val converted = EthUnionCollectionConverter.convert(dto, EthBlockchainDto.ETHEREUM)

        assertThat(converted.owner).isNull()
    }
}