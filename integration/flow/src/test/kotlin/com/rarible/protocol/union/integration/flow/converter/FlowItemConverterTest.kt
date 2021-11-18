package com.rarible.protocol.union.integration.flow.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.MetaAttributeDto
import com.rarible.protocol.dto.MetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowItemConverterTest {

    @Test
    fun `flow item`() {
        val dto = randomFlowNftItemDto()

        val converted = FlowItemConverter.convert(dto, BlockchainDto.FLOW)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.id.contract).isEqualTo(dto.collection)
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)

        // TODO TEZOS Remove when Flow implement getItemRoyalties
        assertThat(converted.royalties[0].value).isEqualTo(FlowConverter.toBasePoints(dto.royalties[0].value))
        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account)

        assertThat(converted.creators[0].value).isEqualTo(FlowConverter.toBasePoints(dto.creators[0].value))
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
                    "url1", "url2"
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

        val originalImage = converted.content[0]
        val bigImage = converted.content[1]

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImage.properties).isNull()

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(bigImage.properties).isNull()
    }
}
