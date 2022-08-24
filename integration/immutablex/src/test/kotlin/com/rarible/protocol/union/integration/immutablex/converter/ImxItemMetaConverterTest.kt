package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.data.randomImxAsset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ImxItemMetaConverterTest {

    private val blockchain = BlockchainDto.IMMUTABLEX

    @Test
    fun `convert item meta`() {
        val imxItem = randomImxAsset().copy(metadata = mapOf("abc" to "1", "error" to "11"))

        val meta = ImxItemMetaConverter.convert(imxItem, setOf("abc"), blockchain)

        assertThat(meta.name).isEqualTo(imxItem.name)
        assertThat(meta.description).isEqualTo(imxItem.description)
        assertThat(meta.createdAt).isEqualTo(imxItem.createdAt)
        assertThat(meta.originalMetaUri).isEqualTo(imxItem.uri)

        assertThat(meta.attributes).hasSize(1)

        assertThat(meta.content).hasSize(1)

        val image = meta.content[0]
        assertThat(image.url).isEqualTo(imxItem.imageUrl)
        assertThat(image.properties).isInstanceOf(UnionImageProperties::class.java)
    }

    @Test
    fun `convert item meta - attributes filtered`() {
        val imxAttributes = mapOf(
            // Should not be present in attributes and content
            "image" to randomString(),
            "image_url" to randomString(),
            // Should not be present in attributes, but should be present in video-content
            "animation_url" to randomString(),
            "youtube_url" to randomString(),
            // Technical info, should be also filtered
            "status" to randomString(),
            "message" to randomString(),
            // Filtered attributes from collection meta schema
            "size" to randomLong(),
            "color" to randomString()
        )
        val imxItem = randomImxAsset().copy(metadata = imxAttributes)

        val meta = ImxItemMetaConverter.convert(imxItem, setOf("size", "color"), blockchain)

        assertThat(meta.attributes).hasSize(2)
        val size = meta.attributes[0]
        assertThat(size.key).isEqualTo("size")
        assertThat(size.value).isEqualTo(imxAttributes["size"].toString())

        val color = meta.attributes[1]
        assertThat(color.key).isEqualTo("color")
        assertThat(color.value).isEqualTo(imxAttributes["color"])

        assertThat(meta.content).hasSize(3)

        val animation = meta.content[0]
        assertThat(animation.url).isEqualTo(imxAttributes["animation_url"])
        assertThat(animation.properties).isInstanceOf(UnionVideoProperties::class.java)

        val youtube = meta.content[1]
        assertThat(youtube.url).isEqualTo(imxAttributes["youtube_url"])
        assertThat(youtube.properties).isInstanceOf(UnionVideoProperties::class.java)

        val imageContent = meta.content[2]
        assertThat(imageContent.url).isEqualTo(imxItem.imageUrl)
        assertThat(imageContent.properties).isInstanceOf(UnionImageProperties::class.java)

    }

}