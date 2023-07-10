package com.rarible.protocol.union.enrichment.meta.simplehash.resolver

import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverterService
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class SimpleHashArtBlocksResolverTest {

    private val simpleHashConverterService = SimpleHashConverterService()
    private val artBlocksResolver = SimpleHashArtBlocksResolver(simpleHashConverterService.mapper)

    @Test
    fun `should support for artblocks - ok`() {
        val source = SimpleHashItem(
            nftId = "ethereum.0x059edd72cd353df5106d2b9cc5ab83a52287ac3a",
            tokenId = null,
            name = null,
            description = null,
            previews = null,
            imageProperties = null,
            extraMetadata = null,
            collection = null,
            createdDate = null,
            externalUrl = null,
        )

        assertTrue(artBlocksResolver.support(source))
    }

    @Test
    fun `should convert artblocks meta - ok`() {
        val raw = this::class.java.getResource("/simplehash/artblocks_1.json").readText()
        val source = simpleHashConverterService.convertRawToSimpleHashItem(raw)
        val converted = simpleHashConverterService.convert(source)

        assertThat(converted).usingRecursiveComparison().isEqualTo(
            UnionMeta(
                name = "Chromie Squiggle #9790",
                description = "Simple and easily identifiable, each squiggle embodies the soul of the Art Blocks platform. Consider each my personal signature as an artist, developer, and tinkerer. Public minting of the Chromie Squiggle is permanently paused. They are now reserved for manual distribution to collectors and community members over a longer period of time. Please visit OpenSea to explore Squiggles available on the secondary market.",
                createdAt = Instant.parse("2023-06-06T23:00:11Z"),
                externalUri = "https://artblocks.io/collections/curated/projects/0x059edd72cd353df5106d2b9cc5ab83a52287ac3a/0/tokens/9790",
                originalMetaUri = "https://api.artblocks.io/token/9790",
                attributes = listOf(
                    UnionMetaAttribute("Type", "Normal"),
                    UnionMetaAttribute("Height", "3"),
                    UnionMetaAttribute("Segments", "15"),
                    UnionMetaAttribute("Spectrum", "Normal"),
                    UnionMetaAttribute("End Color", "233"),
                    UnionMetaAttribute("Start Color", "91"),
                    UnionMetaAttribute("Color Spread", "21"),
                    UnionMetaAttribute("Steps Between", "200"),
                    UnionMetaAttribute("Color Direction", "Reverse"),
                    UnionMetaAttribute("project_id", "0"),
                    UnionMetaAttribute("collection_name", "Chromie Squiggle by Snowfro"),
                ),
                content = listOf(
                    UnionMetaContent(
                        "https://lh3.googleusercontent.com/vlB6dvhEODOrcddWMorSgxlQ4Fl4B8yRCAdkBl66ShqJeBdQ0OjXVHeq7HLqXcdTot3ljt_pEB61WgYAYuWusPlJZVP15xXQcA=s1000",
                        MetaContentDto.Representation.BIG
                    ),
                    UnionMetaContent(
                        "https://lh3.googleusercontent.com/vlB6dvhEODOrcddWMorSgxlQ4Fl4B8yRCAdkBl66ShqJeBdQ0OjXVHeq7HLqXcdTot3ljt_pEB61WgYAYuWusPlJZVP15xXQcA=k-w1200-s2400-rj",
                        MetaContentDto.Representation.PORTRAIT
                    ),
                    UnionMetaContent(
                        "https://lh3.googleusercontent.com/vlB6dvhEODOrcddWMorSgxlQ4Fl4B8yRCAdkBl66ShqJeBdQ0OjXVHeq7HLqXcdTot3ljt_pEB61WgYAYuWusPlJZVP15xXQcA=s250",
                        MetaContentDto.Representation.PREVIEW
                    ),
                    UnionMetaContent(
                        "https://media-proxy.artblocks.io/0x059edd72cd353df5106d2b9cc5ab83a52287ac3a/9790.png",
                        MetaContentDto.Representation.ORIGINAL,
                        null,
                        UnionImageProperties(
                            mimeType = "image/png",
                            size = 234107,
                            available = null,
                            width = 2400,
                            height = 1600
                        )
                    ),
                )
            )
        )
    }

}