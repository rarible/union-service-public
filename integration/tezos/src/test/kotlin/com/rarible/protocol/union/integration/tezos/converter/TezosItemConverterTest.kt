package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.NftItemAttributeDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosItemConverterTest {

    @Test
    fun `tezos item`() {
        val dto = randomTezosNftItemDto()

        val converted = TezosItemConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.date)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value)
    }

    @Test
    fun `tezos item meta`() {
        val item = randomTezosNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                description = randomString(),
                attributes = listOf(
                    NftItemAttributeDto("key1", "value1"),
                    NftItemAttributeDto("key2", "value2")
                ),
                image = "url1",
                animation = "url2"
            )
        )
        val dto = item.meta!!

        val converted = TezosItemConverter.convert(item, BlockchainDto.TEZOS).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0]
        val originalAnim = converted.content[1]
        val emptyImageProperties = UnionImageProperties()
        val emptyVideoProperties = UnionVideoProperties()

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImage.properties).isEqualTo(emptyImageProperties)

        assertThat(originalAnim.url).isEqualTo("url2")
        assertThat(originalAnim.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalAnim.properties).isEqualTo(emptyVideoProperties)
    }

    @Test
    fun `tezos item meta for date type`() {
        val item = randomTezosNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                description = randomString(),
                attributes = listOf(
                    NftItemAttributeDto("key1", "value1"),
                    NftItemAttributeDto("louis_was_here", "1635935214", "date")
                ),
                image = "url1",
                animation = "url2"
            )
        )
        val dto = item.meta!!

        val converted = TezosItemConverter.convert(item, BlockchainDto.TEZOS).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "louis_was_here" }?.value).isEqualTo("2021-11-03T10:26:54Z")
        assertThat(converted.attributes.find { it.key == "louis_was_here" }?.type).isEqualTo("string")
        assertThat(converted.attributes.find { it.key == "louis_was_here" }?.format).isEqualTo("date-time")
    }

    @Test
    fun `tezos item meta for date type with ms precision`() {
        val item = randomTezosNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                description = randomString(),
                attributes = listOf(
                    NftItemAttributeDto("key1", "value1"),
                    NftItemAttributeDto("louis_was_here", "1635935214000", "date")
                ),
                image = "url1",
                animation = "url2"
            )
        )
        val dto = item.meta!!

        val converted = TezosItemConverter.convert(item, BlockchainDto.TEZOS).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "louis_was_here" }?.value).isEqualTo("2021-11-03T10:26:54Z")
        assertThat(converted.attributes.find { it.key == "louis_was_here" }?.type).isEqualTo("string")
        assertThat(converted.attributes.find { it.key == "louis_was_here" }?.format).isEqualTo("date-time")
    }
}
