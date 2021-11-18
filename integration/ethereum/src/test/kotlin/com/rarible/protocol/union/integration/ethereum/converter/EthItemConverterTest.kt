package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemRoyaltyDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthItemConverterTest {

    @Test
    fun `eth item history - transfer`() {
        val dto = randomEthItemTransferDto()

        val converted = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.owner.value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.from.value).isEqualTo(dto.from.prefixed())
    }

    @Test
    fun `eth item history - royalty`() {
        val dto = randomEthItemRoyaltyDto()

        val converted = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account.prefixed())
        assertThat(converted.royalties[0].value).isEqualTo(dto.royalties[0].value)
    }

    @Test
    fun `eth item`() {
        val dto = randomEthNftItemDto()

        val converted = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.id.contract).isEqualTo(dto.contract.prefixed())
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.date)
        assertThat(converted.mintedAt).isEqualTo(dto.date) // TODO not correct
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)

        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account.prefixed())
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value)

        assertThat(converted.pending[0].from.value).isEqualTo(dto.pending!![0].from.prefixed())
        assertThat(converted.pending[0].owner.value).isEqualTo(dto.pending!![0].owner.prefixed())
        assertThat(converted.pending[0].date).isEqualTo(dto.pending!![0].date)
        assertThat(converted.pending[0].value).isEqualTo(dto.pending!![0].value)
        assertThat(converted.pending[0].contract.value).isEqualTo(dto.pending!![0].contract.prefixed())
        assertThat(converted.pending[0].tokenId).isEqualTo(dto.pending!![0].tokenId)
    }

    @Test
    fun `eth item meta`() {
        val item = randomEthNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                description = randomString(),
                attributes = listOf(
                    NftItemAttributeDto("key1", "value1"),
                    NftItemAttributeDto("key2", "value2")
                ),
                image = NftMediaDto(
                    url = LinkedHashMap(mapOf(Pair("ORIGINAL", "url1"), Pair("BIG", "url2"))),
                    meta = mapOf(
                        Pair("ORIGINAL", NftMediaMetaDto("jpeg", 100, 200)),
                        Pair("BIG", NftMediaMetaDto("png", 10, 20))
                    )
                ),
                animation = NftMediaDto(
                    url = LinkedHashMap(mapOf(Pair("ORIGINAL", "url3"), Pair("PREVIEW", "url4"))),
                    meta = mapOf(
                        Pair("ORIGINAL", NftMediaMetaDto("mp4", 200, 400)),
                        Pair("PREVIEW", NftMediaMetaDto("amv", 20, 40))
                    )
                )
            )
        )
        val dto = item.meta!!

        val converted = EthItemConverter.convert(item, BlockchainDto.ETHEREUM).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(4)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0]
        val bigImage = converted.content[1]
        val originalAnim = converted.content[2]
        val previewAnim = converted.content[3]

        val originalImageProperties = originalImage.properties as UnionImageProperties
        val bigImageProperties = bigImage.properties as UnionImageProperties
        val originalAnimProperties = originalAnim.properties as UnionVideoProperties
        val previewAnimProperties = previewAnim.properties as UnionVideoProperties

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImageProperties.mimeType).isEqualTo("jpeg")
        assertThat(originalImageProperties.width).isEqualTo(100)
        assertThat(originalImageProperties.height).isEqualTo(200)

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.representation).isEqualTo(MetaContentDto.Representation.BIG)
        assertThat(bigImageProperties.mimeType).isEqualTo("png")
        assertThat(bigImageProperties.width).isEqualTo(10)
        assertThat(bigImageProperties.height).isEqualTo(20)

        assertThat(originalAnim.url).isEqualTo("url3")
        assertThat(originalAnim.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalAnimProperties.mimeType).isEqualTo("mp4")
        assertThat(originalAnimProperties.width).isEqualTo(200)
        assertThat(originalAnimProperties.height).isEqualTo(400)

        assertThat(previewAnim.url).isEqualTo("url4")
        assertThat(previewAnim.representation).isEqualTo(MetaContentDto.Representation.PREVIEW)
        assertThat(previewAnimProperties.mimeType).isEqualTo("amv")
        assertThat(previewAnimProperties.width).isEqualTo(20)
        assertThat(previewAnimProperties.height).isEqualTo(40)
    }
}
