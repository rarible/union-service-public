package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.test.data.randomEthItemRoyaltyDto
import com.rarible.protocol.union.test.data.randomEthItemTransferDto
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthUnionItemConverterTest {

    @Test
    fun `eth item history - transfer`() {
        val dto = randomEthItemTransferDto()

        val converted = EthUnionItemConverter.convert(dto, BlockchainDto.ETHEREUM)

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

        val converted = EthUnionItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account.prefixed())
        assertThat(converted.royalties[0].value).isEqualTo(EthConverter.convertFromBp(dto.royalties[0].value))
    }

    @Test
    fun `eth item`() {
        val dto = randomEthNftItemDto()

        val converted = EthUnionItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.id.token.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.date)
        assertThat(converted.mintedAt).isEqualTo(dto.date) // TODO not correct
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)

        assertThat(converted.owners[0].value).isEqualTo(dto.owners[0].prefixed())

        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account.prefixed())
        assertThat(converted.royalties[0].value).isEqualTo(EthConverter.convertFromBp(dto.royalties[0].value))

        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account.prefixed())
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value.toBigDecimal())

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
                        Pair("PREVIEW", NftMediaMetaDto("gif", 20, 40))
                    )
                )
            )
        )
        val dto = item.meta!!

        val converted = EthUnionItemConverter.convert(item, BlockchainDto.ETHEREUM).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.contents).hasSize(4)

        val originalImage = converted.contents[0]
        val bigImage = converted.contents[1]
        val originalAnim = converted.contents[2]
        val previewAnim = converted.contents[3]

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

        assertThat(originalAnim.url).isEqualTo("url3")
        assertThat(originalAnim.typeContent).isEqualTo("ORIGINAL")
        assertThat(originalAnim.attributes.find { it.key == "type" }!!.value).isEqualTo("mp4")
        assertThat(originalAnim.attributes.find { it.key == "width" }!!.value).isEqualTo("200")
        assertThat(originalAnim.attributes.find { it.key == "height" }!!.value).isEqualTo("400")

        assertThat(previewAnim.url).isEqualTo("url4")
        assertThat(previewAnim.typeContent).isEqualTo("PREVIEW")
        assertThat(previewAnim.attributes.find { it.key == "type" }!!.value).isEqualTo("gif")
        assertThat(previewAnim.attributes.find { it.key == "width" }!!.value).isEqualTo("20")
        assertThat(previewAnim.attributes.find { it.key == "height" }!!.value).isEqualTo("40")
    }
}