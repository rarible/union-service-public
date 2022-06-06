package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.Model3dContentDto
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.UnknownContentDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
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
        assertThat(converted.collection!!.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
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
                language = randomString(),
                genres = listOf(randomString(), randomString()),
                tags = listOf(randomString(), randomString()),
                createdAt = nowMillis(),
                rights = randomString(),
                rightsUri = randomString(),
                externalUri = randomString(),
                attributes = listOf(
                    NftItemAttributeDto("key1", "value1"),
                    NftItemAttributeDto("key2", "value2")
                ),
                image = null,
                animation = null
            )
        )
        val dto = item.meta!!

        val converted = EthItemConverter.convert(item, BlockchainDto.ETHEREUM).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.genres).isEqualTo(dto.genres)
        assertThat(converted.tags).isEqualTo(dto.tags)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.rightsUri).isEqualTo(dto.rightsUri)
        assertThat(converted.rights).isEqualTo(dto.rights)
        assertThat(converted.externalUri).isEqualTo(dto.externalUri)
        assertThat(converted.content).hasSize(0)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")
    }

    @Test
    fun `eth item meta content`() {
        val imageContent = ImageContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.ORIGINAL,
            mimeType = "image/jpeg",
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )
        val videoContent = VideoContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.BIG,
            mimeType = "video/mp4",
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )
        val audioContent = AudioContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.PREVIEW,
            mimeType = "audio/mp3",
            size = randomLong()
        )
        val modelContent = Model3dContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.ORIGINAL,
            mimeType = "model/gltf+json",
            size = randomLong()
        )
        val htmlContent = HtmlContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.ORIGINAL,
            mimeType = "text/html",
            size = 100
        )
        val unknownContent = UnknownContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.ORIGINAL,
            mimeType = "text/plain",
            size = randomLong()
        )

        val item = randomEthNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                content = listOf(
                    imageContent,
                    videoContent,
                    audioContent,
                    modelContent,
                    htmlContent,
                    unknownContent
                )
            )
        )

        val converted = EthItemConverter.convert(item, BlockchainDto.ETHEREUM).meta!!

        val image = converted.content[0]
        val video = converted.content[1]
        val audio = converted.content[2]
        val model = converted.content[3]
        val html = converted.content[4]
        val unknown = converted.content[5]

        val imageProperties = image.properties as UnionImageProperties
        val videoProperties = video.properties as UnionVideoProperties
        val audioProperties = audio.properties as UnionAudioProperties
        val modelProperties = model.properties as UnionModel3dProperties
        val htmlProperties = html.properties as UnionHtmlProperties
        val unknownProperties = unknown.properties as UnionUnknownProperties

        assertThat(image.url).isEqualTo(imageContent.url)
        assertThat(image.fileName).isEqualTo(imageContent.fileName)
        assertThat(image.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(imageProperties.mimeType).isEqualTo(imageContent.mimeType)
        assertThat(imageProperties.width).isEqualTo(imageContent.width)
        assertThat(imageProperties.height).isEqualTo(imageContent.height)
        assertThat(imageProperties.size).isEqualTo(imageContent.size)

        assertThat(video.url).isEqualTo(videoContent.url)
        assertThat(video.fileName).isEqualTo(videoContent.fileName)
        assertThat(video.representation).isEqualTo(MetaContentDto.Representation.BIG)
        assertThat(videoProperties.mimeType).isEqualTo(videoContent.mimeType)
        assertThat(videoProperties.width).isEqualTo(videoContent.width)
        assertThat(videoProperties.height).isEqualTo(videoContent.height)
        assertThat(videoProperties.size).isEqualTo(videoContent.size)

        assertThat(audio.url).isEqualTo(audioContent.url)
        assertThat(audio.fileName).isEqualTo(audioContent.fileName)
        assertThat(audio.representation).isEqualTo(MetaContentDto.Representation.PREVIEW)
        assertThat(audioProperties.mimeType).isEqualTo(audioContent.mimeType)
        assertThat(audioProperties.size).isEqualTo(audioContent.size)

        assertThat(model.url).isEqualTo(modelContent.url)
        assertThat(model.fileName).isEqualTo(modelContent.fileName)
        assertThat(model.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(modelProperties.mimeType).isEqualTo(modelContent.mimeType)
        assertThat(modelProperties.size).isEqualTo(modelProperties.size)

        assertThat(html.url).isEqualTo(htmlContent.url)
        assertThat(html.fileName).isEqualTo(htmlContent.fileName)
        assertThat(html.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(htmlProperties.mimeType).isEqualTo(htmlContent.mimeType)
        assertThat(htmlProperties.size).isEqualTo(htmlProperties.size)

        assertThat(unknown.url).isEqualTo(unknownContent.url)
        assertThat(unknown.fileName).isEqualTo(unknownContent.fileName)
        assertThat(unknown.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(unknownProperties.mimeType).isEqualTo(unknownContent.mimeType)
        assertThat(unknownProperties.size).isEqualTo(unknownProperties.size)
    }

    @Test
    fun `eth item meta content - legacy is not preferred`() {
        val imageContent = ImageContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = com.rarible.protocol.dto.MetaContentDto.Representation.ORIGINAL,
            mimeType = "image/jpeg",
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )

        val item = randomEthNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                content = listOf(imageContent),
                image = NftMediaDto(
                    url = LinkedHashMap(mapOf(Pair("ORIGINAL", "url1"), Pair("BIG", "url2"))),
                    meta = mapOf(
                        Pair("ORIGINAL", NftMediaMetaDto("jpeg", 100, 200)),
                        Pair("BIG", NftMediaMetaDto("png", 10, 20))
                    )
                )
            )
        )

        val converted = EthItemConverter.convert(item, BlockchainDto.ETHEREUM).meta!!

        assertThat(converted.content).hasSize(1)

        val image = converted.content[0]
        val imageProperties = image.properties as UnionImageProperties

        assertThat(image.url).isEqualTo(imageContent.url)
        assertThat(image.fileName).isEqualTo(imageContent.fileName)
        assertThat(image.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(imageProperties.mimeType).isEqualTo(imageContent.mimeType)
        assertThat(imageProperties.width).isEqualTo(imageContent.width)
        assertThat(imageProperties.height).isEqualTo(imageContent.height)
        assertThat(imageProperties.size).isEqualTo(imageContent.size)
    }

    @Test
    fun `eth item meta content - legacy`() {
        val item = randomEthNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
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

        val converted = EthItemConverter.convert(item, BlockchainDto.ETHEREUM).meta!!

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
