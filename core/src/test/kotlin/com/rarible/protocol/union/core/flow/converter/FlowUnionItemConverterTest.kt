package com.rarible.protocol.union.core.flow.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.MetaAttributeDto
import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.dto.MetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowUnionItemConverterTest {

    @Test
    fun `flow item`() {
        val dto = randomFlowNftItemDto()

        val converted = FlowUnionItemConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.collection.value).isEqualTo(dto.collection)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt)
        assertThat(converted.metaUrl).isEqualTo(dto.metaUrl)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)

        assertThat(converted.owners.map { it.value }).isEqualTo(dto.owners)
        assertThat(converted.royalties[0].value).isEqualTo(dto.royalties[0].value)
        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account)

        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value)
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
    }

    @Test
    fun `flow item meta`() {
        val item = randomFlowNftItemDto().copy(
            meta = MetaDto(
                name = "some_nft_meta",
                description = randomString(),
                raw = randomString(),
                attributes = listOf(
                    MetaAttributeDto("key1", "value1"),
                    MetaAttributeDto("key2", "value2")
                ),
                contents = listOf(
                    MetaContentDto(
                        contentType = "ORIGINAL",
                        url = "url1",
                        attributes = listOf(
                            MetaAttributeDto("width", "100"),
                            MetaAttributeDto("height", "200"),
                            MetaAttributeDto("type", "jpeg")
                        )
                    ),
                    MetaContentDto(
                        contentType = "BIG",
                        url = "url2",
                        attributes = listOf(
                            MetaAttributeDto("width", "10"),
                            MetaAttributeDto("height", "20"),
                            MetaAttributeDto("type", "png")
                        )
                    )
                )
            )
        )
        val dto = item.meta!!

        val converted = FlowUnionItemConverter.convert(item, BlockchainDto.FLOW).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.raw).isEqualTo(dto.raw)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.contents).hasSize(2)

        val originalImage = converted.contents[0]
        val bigImage = converted.contents[1]

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.typeContent).isEqualTo("ORIGINAL")
        assertThat(originalImage.attributes.find { it.key == "type" }!!.value).isEqualTo("jpeg")
        assertThat(originalImage.attributes.find { it.key == "width" }!!.value).isEqualTo("100")
        assertThat(originalImage.attributes.find { it.key == "height" }!!.value).isEqualTo("200")

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.typeContent).isEqualTo("BIG")
        assertThat(bigImage.attributes.find { it.key == "type" }!!.value).isEqualTo("png")
        assertThat(bigImage.attributes.find { it.key == "width" }!!.value).isEqualTo("10")
        assertThat(bigImage.attributes.find { it.key == "height" }!!.value).isEqualTo("20")
    }


}