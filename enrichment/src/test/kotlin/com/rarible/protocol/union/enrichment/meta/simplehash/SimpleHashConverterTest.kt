package com.rarible.protocol.union.enrichment.meta.simplehash

import com.simplehash.v0.image_properties
import com.simplehash.v0.nft
import com.simplehash.v0.previews
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleHashConverterTest {

    @Test
    fun `convert kafka event - ok`() {
        val event = nft.newBuilder()
            .setNftId("ethereum-goerli.0x2e2b1904eed0a58b1e2efb3df70216d439abce8b.3533")
            .setTokenId("3533")
            .setDescription("desc")
            .setPreviewsBuilder(
                previews.newBuilder()
                    .setImageSmallUrl("small")
                    .setImageMediumUrl("medium")
                    .setImageLargeUrl("large")
                    .setImageOpengraphUrl("open")
            )
            .setImagePropertiesBuilder(
                image_properties.newBuilder()
                    .setHeight(100)
                    .setWidth(200)
                    .setSize(500)
                    .setMimeType("png")
            )
            .setCreatedDate("2023-07-18T19:56")
            .setExternalUrl("http://localhost/extra")
            .setExtraMetadata("""{\"attributes\": [], \"image_original_url\": \"http://localhost\", \"animation_original_url\": null, \"metadata_original_url\": null}""")
            .build()

        val item = SimpleHashConverter.convert(event)

        assertThat(item.nftId).isEqualTo(event.nftId)
        assertThat(item.tokenId).isEqualTo(event.tokenId)
        assertThat(item.description).isEqualTo(event.description)
        assertThat(item.previews).isEqualTo(
            SimpleHashItem.Preview(
                imageSmallUrl = event.previews.imageSmallUrl.toString(),
                imageMediumUrl = event.previews.imageMediumUrl.toString(),
                imageLargeUrl = event.previews.imageLargeUrl.toString(),
                imageOpengraphUrl = event.previews.imageOpengraphUrl.toString()
            )
        )
        assertThat(item.imageProperties).isEqualTo(
            SimpleHashItem.ImageProperties(
                width = event.imageProperties.width,
                height = event.imageProperties.height,
                size = event.imageProperties.size.toLong(),
                mimeType = event.imageProperties.mimeType.toString()
            )
        )
        assertThat(item.createdDate.toString()).isEqualTo(event.createdDate)
        assertThat(item.externalUrl).isEqualTo(event.externalUrl)
        assertThat(item.extraMetadata).isEqualTo(
            SimpleHashItem.ExtraMetadata(
                attributes = emptyList(),
                imageOriginalUrl = "http://localhost"
            )
        )
    }
}
