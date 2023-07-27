package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.dto.AudioContentDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.Model3dContentDto
import com.rarible.protocol.union.dto.VideoContentDto
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollectionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EnrichedMetaConverterTest {

    @Test
    fun `convert item meta`() {
        val entry = randomItemMetaDownloadEntry()
        val meta = entry.data!!
        val converted = MetaDtoConverter.convert(entry)!!

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.createdAt).isEqualTo(meta.createdAt)
        assertThat(converted.updatedAt).isEqualTo(entry.succeedAt)
        assertThat(converted.tags).isEqualTo(meta.tags)
        assertThat(converted.genres).isEqualTo(meta.genres)
        assertThat(converted.language).isEqualTo(meta.language)
        assertThat(converted.rights).isEqualTo(meta.rights)
        assertThat(converted.rightsUri).isEqualTo(meta.rightsUri)
        assertThat(converted.externalUri).isEqualTo(meta.externalUri)
        assertThat(converted.originalMetaUri).isEqualTo(meta.originalMetaUri)
        assertThat(converted.attributes).isEqualTo(meta.attributes.map { MetaDtoConverter.convert(it) })
        assertThat(converted.restrictions).isEqualTo(meta.restrictions.map { it.type })
    }

    @Test
    fun `convert item meta - failed entry`() {
        val entry = randomItemMetaDownloadEntry(data = null, status = DownloadStatus.FAILED)
        val converted = MetaDtoConverter.convert(entry)
        assertThat(converted).isNull()
    }

    @Test
    fun `convert collection meta`() {
        val meta = randomUnionCollectionMeta()

        val converted = MetaDtoConverter.convert(meta)

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.createdAt).isEqualTo(meta.createdAt)
        assertThat(converted.tags).isEqualTo(meta.tags)
        assertThat(converted.genres).isEqualTo(meta.genres)
        assertThat(converted.language).isEqualTo(meta.language)
        assertThat(converted.rights).isEqualTo(meta.rights)
        assertThat(converted.rightsUri).isEqualTo(meta.rightsUri)
        assertThat(converted.externalUri).isEqualTo(meta.externalUri)
        assertThat(converted.originalMetaUri).isEqualTo(meta.originalMetaUri)

        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.description).isEqualTo(meta.description)
    }

    @Test
    fun `convert image properties`() {
        val imageProperties = UnionImageProperties(
            mimeType = "image/png",
            size = randomLong(),
            width = randomInt(),
            height = randomInt()
        )
        val image = randomUnionContent(imageProperties)

        val meta = randomUnionMeta().copy(content = listOf(image))

        val converted = MetaDtoConverter.convert(meta)
        val convertedImage = converted.content[0] as ImageContentDto

        assertThat(convertedImage.url).isEqualTo(image.url)
        assertThat(convertedImage.fileName).isEqualTo(image.fileName)
        assertThat(convertedImage.representation).isEqualTo(image.representation)
        assertThat(convertedImage.mimeType).isEqualTo(imageProperties.mimeType)
        assertThat(convertedImage.size).isEqualTo(imageProperties.size)
        assertThat(convertedImage.width).isEqualTo(imageProperties.width)
        assertThat(convertedImage.height).isEqualTo(imageProperties.height)
    }

    @Test
    fun `convert video properties`() {
        val videoProperties = UnionVideoProperties(
            mimeType = "video/mp4",
            size = randomLong(),
            width = randomInt(),
            height = randomInt()
        )
        val video = randomUnionContent(videoProperties)

        val meta = randomUnionMeta().copy(content = listOf(video))

        val converted = MetaDtoConverter.convert(meta)
        val convertedVideo = converted.content[0] as VideoContentDto

        assertThat(convertedVideo.url).isEqualTo(video.url)
        assertThat(convertedVideo.fileName).isEqualTo(video.fileName)
        assertThat(convertedVideo.representation).isEqualTo(video.representation)
        assertThat(convertedVideo.mimeType).isEqualTo(videoProperties.mimeType)
        assertThat(convertedVideo.size).isEqualTo(videoProperties.size)
        assertThat(convertedVideo.width).isEqualTo(videoProperties.width)
        assertThat(convertedVideo.height).isEqualTo(videoProperties.height)
    }

    @Test
    fun `convert audio properties`() {
        val audioProperties = UnionAudioProperties(
            mimeType = "audio/mp4",
            size = randomLong()
        )
        val audio = randomUnionContent(audioProperties)

        val meta = randomUnionMeta().copy(content = listOf(audio))

        val converted = MetaDtoConverter.convert(meta)
        val convertedAudio = converted.content[0] as AudioContentDto

        assertThat(convertedAudio.url).isEqualTo(audio.url)
        assertThat(convertedAudio.fileName).isEqualTo(audio.fileName)
        assertThat(convertedAudio.representation).isEqualTo(audio.representation)
        assertThat(convertedAudio.mimeType).isEqualTo(audioProperties.mimeType)
        assertThat(convertedAudio.size).isEqualTo(audioProperties.size)
    }

    @Test
    fun `convert model properties`() {
        val modelProperties = UnionModel3dProperties(
            mimeType = "model/gltf",
            size = randomLong()
        )
        val model = randomUnionContent(modelProperties)

        val meta = randomUnionMeta().copy(content = listOf(model))

        val converted = MetaDtoConverter.convert(meta)
        val convertedModel = converted.content[0] as Model3dContentDto

        assertThat(convertedModel.url).isEqualTo(model.url)
        assertThat(convertedModel.fileName).isEqualTo(model.fileName)
        assertThat(convertedModel.representation).isEqualTo(model.representation)
        assertThat(convertedModel.mimeType).isEqualTo(modelProperties.mimeType)
        assertThat(convertedModel.size).isEqualTo(modelProperties.size)
    }
}
