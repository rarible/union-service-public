package com.rarible.protocol.union.core.flow.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.MetaAttributeDto
import com.rarible.protocol.dto.MetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FlowItemConverterTest {

    @Test
    fun `flow item`() {
        val dto = randomFlowNftItemDto()

        val converted = FlowItemConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.collection.value).isEqualTo(dto.collection)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)

        assertThat(converted.owners.map { it.value }).isEqualTo(dto.owners)
        assertThat(converted.royalties[0].value).isEqualTo(dto.royalties[0].value)
        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account)

        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value)
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
    }

    @Test
    @Disabled
    // TODO enable when Flow update Meta model
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
                    com.rarible.protocol.dto.MetaContentDto(
                        contentType = "ORIGINAL",
                        url = "url1",
                        attributes = listOf(
                            MetaAttributeDto("width", "100"),
                            MetaAttributeDto("height", "200"),
                            MetaAttributeDto("type", "jpeg")
                        )
                    ),
                    com.rarible.protocol.dto.MetaContentDto(
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

        val converted = FlowItemConverter.convert(item, BlockchainDto.FLOW).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0] as ImageContentDto
        val bigImage = converted.content[1] as ImageContentDto

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImage.mimeType).isEqualTo("jpeg")
        assertThat(originalImage.width).isEqualTo(100)
        assertThat(originalImage.height).isEqualTo(200)

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.representation).isEqualTo(MetaContentDto.Representation.BIG)
        assertThat(bigImage.mimeType).isEqualTo("png")
        assertThat(bigImage.width).isEqualTo(10)
        assertThat(bigImage.height).isEqualTo(20)
    }


}
