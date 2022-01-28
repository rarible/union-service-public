package com.rarible.protocol.union.listener.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import com.rarible.protocol.union.enrichment.meta.toImageProperties
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@IntegrationTest
class ContentMetaServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var contentMetaService: ContentMetaService

    @Autowired
    @Qualifier("test.content.meta.loader")
    private lateinit var testContentMetaLoader: ContentMetaLoader

    @Test
    fun `enrich with content meta`() = runBlocking<Unit> {
        val url = "https://image.com/some.jpg"
        val contentMeta = ContentMeta(
            type = "image/jpg",
            width = 10,
            height = 20,
            size = 30
        )
        coEvery { testContentMetaLoader.fetchContentMeta(url) } returns contentMeta
        val enriched = contentMetaService.enrichWithContentMeta(
            UnionMetaContent(
                url = url,
                representation = MetaContentDto.Representation.ORIGINAL,
                properties = null
            )
        )
        assertThat(enriched).isEqualTo(
            enriched.copy(properties = contentMeta.toImageProperties())
        )
    }
}
