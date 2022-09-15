package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosDipDupItemDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosTzktItemDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupItemConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktItemConverter
import com.rarible.tzkt.model.TokenMeta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosItemConverterTest {

    @Test
    fun `tezos tzkt item`() {
        val dto = randomTezosTzktItemDto()

        val converted = TzktItemConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.itemId())
        assertThat(converted.collection!!.value).isEqualTo(dto.contract?.address)
        assertThat(converted.supply).isEqualTo(dto.totalSupply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastTime?.toInstant())
        assertThat(converted.mintedAt).isEqualTo(dto.firstTime?.toInstant())
        assertThat(converted.deleted).isEqualTo(dto.isDeleted())

        // We set creators later with additional query
        assertThat(converted.creators).isEqualTo(emptyList<CreatorDto>())
    }

    @Test
    fun `tezos tzkt item meta`() {
        val item = randomTezosTzktItemDto().copy(
            meta = TokenMeta(
                name = "some_nft_meta",
                description = randomString(),
                attributes = listOf(
                    TokenMeta.Attribute("key1", "value1"),
                    TokenMeta.Attribute("key2", "value2")
                ),
                content = listOf(
                    TokenMeta.Content("url1", "jpeg", TokenMeta.Representation.ORIGINAL)
                )
            )
        )
        val dto = item.meta!!

        val converted = TzktItemConverter.convert(item, BlockchainDto.TEZOS).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(1)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0]
        val emptyImageProperties = UnionImageProperties()

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImage.properties).isNull()
    }

    @Test
    fun `tezos dipdup item`() {
        val dto = randomTezosDipDupItemDto()

        val converted = DipDupItemConverter.convert(dto)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.updated)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
    }
}
