package com.rarible.protocol.union.enrichment.meta.simplehash

import com.simplehash.v0.nft
import com.simplehash.v0.previews
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleHashConverterTest {

    @Test
    fun `convert kafka event - ok`() {
        val event = nft.newBuilder()
            .setNftId("ethereum-goerli.0x2e2b1904eed0a58b1e2efb3df70216d439abce8b.3533")
            .setPreviewsBuilder(previews.newBuilder()
                .setImageSmallUrl("small")
                .setImageMediumUrl("medium")
                .setImageLargeUrl("large")
                .setImageOpengraphUrl("open")
            )
            .setExtraMetadata("""{\"attributes\": [], \"image_original_url\": \"http://localhost\", \"animation_original_url\": null, \"metadata_original_url\": null}""")
            .build()

        val item = SimpleHashConverter.convert(event)

        assertThat(item.nftId).isEqualTo(event.nftId)
        assertThat(item.previews).isEqualTo(SimpleHashItem.Preview(
            imageSmallUrl = "small",
            imageMediumUrl = "medium",
            imageLargeUrl = "large",
            imageOpengraphUrl = "open"
        ))
        assertThat(item.extraMetadata).isEqualTo(SimpleHashItem.ExtraMetadata(
            attributes = emptyList(),
            imageOriginalUrl = "http://localhost"
        ))
    }

}