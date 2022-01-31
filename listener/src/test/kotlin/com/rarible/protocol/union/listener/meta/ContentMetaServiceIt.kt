package com.rarible.protocol.union.listener.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import com.rarible.protocol.union.enrichment.meta.toImageProperties
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.time.Duration

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
        val enriched = contentMetaService.fetchContentMetaWithTimeout(url, Duration.ofSeconds(1))
        assertThat(enriched).isEqualTo(contentMeta.toImageProperties())
    }

    @Test
    fun `enrich with content meta - null on timeout`() = runBlocking<Unit> {
        val url = "https://image.com/some.jpg"
        coEvery { testContentMetaLoader.fetchContentMeta(url) } coAnswers {
            delay(10000)
            error("will not happen")
        }
        assertThat(contentMetaService.fetchContentMetaWithTimeout(url, Duration.ofSeconds(1))).isNull()
    }

}
