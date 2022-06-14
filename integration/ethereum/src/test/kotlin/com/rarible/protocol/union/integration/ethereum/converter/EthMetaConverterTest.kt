package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaContentDto
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
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionMetaDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMeta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthMetaConverterTest {

    @Test
    fun `eth item meta`() {
        val meta = NftItemMetaDto(
            name = "some_nft_meta",
            description = randomString(),
            language = randomString(),
            genres = listOf(randomString(), randomString()),
            tags = listOf(randomString(), randomString()),
            createdAt = nowMillis(),
            rights = randomString(),
            rightsUri = randomString(),
            externalUri = randomString(),
            originalMetaUri = randomString(),
            attributes = listOf(
                NftItemAttributeDto("key1", "value1"),
                NftItemAttributeDto("key2", "value2")
            ),
            image = null,
            animation = null,
            content = listOf()
        )

        val converted = EthMetaConverter.convert(meta)

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.genres).isEqualTo(meta.genres)
        assertThat(converted.tags).isEqualTo(meta.tags)
        assertThat(converted.createdAt).isEqualTo(meta.createdAt)
        assertThat(converted.rightsUri).isEqualTo(meta.rightsUri)
        assertThat(converted.rights).isEqualTo(meta.rights)
        assertThat(converted.externalUri).isEqualTo(meta.externalUri)
        assertThat(converted.originalMetaUri).isEqualTo(meta.originalMetaUri)
        assertThat(converted.content).hasSize(0)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")
    }

    @Test
    fun `eth item meta content`() {
        val imageContent = ImageContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.ORIGINAL,
            mimeType = "image/jpeg",
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )
        val videoContent = VideoContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.BIG,
            mimeType = "video/mp4",
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )
        val audioContent = AudioContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.PREVIEW,
            mimeType = "audio/mp3",
            size = randomLong()
        )
        val modelContent = Model3dContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.ORIGINAL,
            mimeType = "model/gltf+json",
            size = randomLong()
        )
        val htmlContent = HtmlContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.ORIGINAL,
            mimeType = "text/html",
            size = 100
        )
        val unknownContent = UnknownContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.ORIGINAL,
            mimeType = "text/plain",
            size = randomLong()
        )

        val meta = randomEthItemMeta().copy(
            content = listOf(
                imageContent,
                videoContent,
                audioContent,
                modelContent,
                htmlContent,
                unknownContent
            )
        )

        val converted = EthMetaConverter.convert(meta)

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
        assertThat(image.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(imageProperties.mimeType).isEqualTo(imageContent.mimeType)
        assertThat(imageProperties.width).isEqualTo(imageContent.width)
        assertThat(imageProperties.height).isEqualTo(imageContent.height)
        assertThat(imageProperties.size).isEqualTo(imageContent.size)

        assertThat(video.url).isEqualTo(videoContent.url)
        assertThat(video.fileName).isEqualTo(videoContent.fileName)
        assertThat(video.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.BIG)
        assertThat(videoProperties.mimeType).isEqualTo(videoContent.mimeType)
        assertThat(videoProperties.width).isEqualTo(videoContent.width)
        assertThat(videoProperties.height).isEqualTo(videoContent.height)
        assertThat(videoProperties.size).isEqualTo(videoContent.size)

        assertThat(audio.url).isEqualTo(audioContent.url)
        assertThat(audio.fileName).isEqualTo(audioContent.fileName)
        assertThat(audio.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.PREVIEW)
        assertThat(audioProperties.mimeType).isEqualTo(audioContent.mimeType)
        assertThat(audioProperties.size).isEqualTo(audioContent.size)

        assertThat(model.url).isEqualTo(modelContent.url)
        assertThat(model.fileName).isEqualTo(modelContent.fileName)
        assertThat(model.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(modelProperties.mimeType).isEqualTo(modelContent.mimeType)
        assertThat(modelProperties.size).isEqualTo(modelProperties.size)

        assertThat(html.url).isEqualTo(htmlContent.url)
        assertThat(html.fileName).isEqualTo(htmlContent.fileName)
        assertThat(html.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(htmlProperties.mimeType).isEqualTo(htmlContent.mimeType)
        assertThat(htmlProperties.size).isEqualTo(htmlProperties.size)

        assertThat(unknown.url).isEqualTo(unknownContent.url)
        assertThat(unknown.fileName).isEqualTo(unknownContent.fileName)
        assertThat(unknown.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(unknownProperties.mimeType).isEqualTo(unknownContent.mimeType)
        assertThat(unknownProperties.size).isEqualTo(unknownProperties.size)
    }

    @Test
    fun `eth item meta content - legacy is not preferred`() {
        val imageContent = ImageContentDto(
            url = randomString(),
            fileName = randomString(),
            representation = MetaContentDto.Representation.ORIGINAL,
            mimeType = "image/jpeg",
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )

        val meta = NftItemMetaDto(
            name = "some_nft_meta",
            content = listOf(imageContent),
            tags = emptyList(),
            genres = emptyList(),
            image = NftMediaDto(
                url = LinkedHashMap(mapOf(Pair("ORIGINAL", "url1"), Pair("BIG", "url2"))),
                meta = mapOf(
                    Pair("ORIGINAL", NftMediaMetaDto("jpeg", 100, 200)),
                    Pair("BIG", NftMediaMetaDto("png", 10, 20))
                )
            )
        )

        val converted = EthMetaConverter.convert(meta)

        assertThat(converted.content).hasSize(1)

        val image = converted.content[0]
        val imageProperties = image.properties as UnionImageProperties

        assertThat(image.url).isEqualTo(imageContent.url)
        assertThat(image.fileName).isEqualTo(imageContent.fileName)
        assertThat(image.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(imageProperties.mimeType).isEqualTo(imageContent.mimeType)
        assertThat(imageProperties.width).isEqualTo(imageContent.width)
        assertThat(imageProperties.height).isEqualTo(imageContent.height)
        assertThat(imageProperties.size).isEqualTo(imageContent.size)
    }

    @Test
    fun `eth item meta content - legacy`() {
        val meta = NftItemMetaDto(
            name = "some_nft_meta",
            tags = emptyList(),
            genres = emptyList(),
            content = emptyList(),
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

        val converted = EthMetaConverter.convert(meta)

        val originalImage = converted.content[0]
        val bigImage = converted.content[1]
        val originalAnim = converted.content[2]
        val previewAnim = converted.content[3]

        val originalImageProperties = originalImage.properties as UnionImageProperties
        val bigImageProperties = bigImage.properties as UnionImageProperties
        val originalAnimProperties = originalAnim.properties as UnionVideoProperties
        val previewAnimProperties = previewAnim.properties as UnionVideoProperties

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImageProperties.mimeType).isEqualTo("jpeg")
        assertThat(originalImageProperties.width).isEqualTo(100)
        assertThat(originalImageProperties.height).isEqualTo(200)

        assertThat(bigImage.url).isEqualTo("url2")
        assertThat(bigImage.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.BIG)
        assertThat(bigImageProperties.mimeType).isEqualTo("png")
        assertThat(bigImageProperties.width).isEqualTo(10)
        assertThat(bigImageProperties.height).isEqualTo(20)

        assertThat(originalAnim.url).isEqualTo("url3")
        assertThat(originalAnim.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL)
        assertThat(originalAnimProperties.mimeType).isEqualTo("mp4")
        assertThat(originalAnimProperties.width).isEqualTo(200)
        assertThat(originalAnimProperties.height).isEqualTo(400)

        assertThat(previewAnim.url).isEqualTo("url4")
        assertThat(previewAnim.representation)
            .isEqualTo(com.rarible.protocol.union.dto.MetaContentDto.Representation.PREVIEW)
        assertThat(previewAnimProperties.mimeType).isEqualTo("amv")
        assertThat(previewAnimProperties.width).isEqualTo(20)
        assertThat(previewAnimProperties.height).isEqualTo(40)
    }

    @Test
    fun `eth collection meta`() {
        val meta = randomEthCollectionMetaDto()

        val converted = EthMetaConverter.convert(meta, BlockchainDto.ETHEREUM)!!

        assertThat(converted).isNotNull()

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.genres).isEqualTo(meta.genres)
        assertThat(converted.tags).isEqualTo(meta.tags)
        assertThat(converted.createdAt).isEqualTo(meta.createdAt)
        assertThat(converted.rightsUri).isEqualTo(meta.rightsUri)
        assertThat(converted.rights).isEqualTo(meta.rights)
        assertThat(converted.externalUri).isEqualTo(meta.externalUri)
        assertThat(converted.originalMetaUri).isEqualTo(meta.originalMetaUri)
        assertThat(converted.content).hasSize(1)
        val contentImage = converted.content.first()

        assertThat(contentImage).isExactlyInstanceOf(UnionMetaContent::class.java)
        val originalUrl = meta.image!!.url.values.first()
        val originalMediaMeta = meta.image!!.meta.values.first()
        assertThat(contentImage.url).isEqualTo(originalUrl)
        assertThat(contentImage.representation).isEqualTo(
            com.rarible.protocol.union.dto.MetaContentDto.Representation.ORIGINAL
        )

        val properties = contentImage.properties!!
        assertThat(properties).isExactlyInstanceOf(UnionImageProperties::class.java)
        properties as UnionImageProperties
        assertThat(properties.mimeType).isEqualTo(originalMediaMeta.type)
        assertThat(properties.width).isEqualTo(originalMediaMeta.width)
        assertThat(properties.height).isEqualTo(originalMediaMeta.height)
        assertThat(properties.size).isNull()
    }

}