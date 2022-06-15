package com.rarible.protocol.union.integration.flow.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowImageContentDto
import com.rarible.protocol.dto.FlowMetaAttributeDto
import com.rarible.protocol.dto.FlowMetaContentItemDto
import com.rarible.protocol.dto.FlowMetaDto
import com.rarible.protocol.dto.FlowVideoContentDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.flow.data.randomFlowNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowItemConverterTest {

    @Test
    fun `flow item`() {
        val dto = randomFlowNftItemDto()

        val converted = FlowItemConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.collection!!.value).isEqualTo(dto.collection)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
        assertThat(converted.creators[0].value).isEqualTo(FlowConverter.toBasePoints(dto.creators[0].value))
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
    }

    @Test
    fun `flow item old meta`() {
        val meta = FlowMetaDto(
            name = "some_nft_meta",
            description = randomString(),
            raw = randomString(),
            attributes = listOf(
                FlowMetaAttributeDto("key1", "value1"),
                FlowMetaAttributeDto("key2", "value2")
            ),
            contents = listOf(
                "url1", "url2"
            )
        )

        val converted = FlowItemConverter.convert(meta)

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0]
        val bigImage = converted.content[1]

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImage.properties).isNull()

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(bigImage.properties).isNull()
    }

    @Test
    fun `flow item meta`() {
        val meta = FlowMetaDto(
            name = "some_nft_meta",
            description = randomString(),
            raw = randomString(),
            attributes = listOf(
                FlowMetaAttributeDto("key1", "value1"),
                FlowMetaAttributeDto("key2", "value2")
            ),
            content = listOf(
                FlowImageContentDto(
                    url = "url1",
                    representation = FlowMetaContentItemDto.Representation.PREVIEW,
                    fileName = "image1.png",
                ),
                FlowVideoContentDto(
                    url = "url2",
                    representation = FlowMetaContentItemDto.Representation.ORIGINAL,
                    width = 380,
                    height = 240,
                )
            )
        )

        val converted = FlowItemConverter.convert(meta)

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0]
        val bigImage = converted.content[1]

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.PREVIEW)
        assertThat(originalImage.properties).isNull()

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(bigImage.properties).isNull()
    }
}
