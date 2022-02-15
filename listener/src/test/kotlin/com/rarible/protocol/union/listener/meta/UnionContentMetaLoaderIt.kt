package com.rarible.protocol.union.listener.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.enrichment.meta.ContentMetaEntry
import com.rarible.protocol.union.enrichment.meta.ContentMetaEntry.Companion.CACHE_META_TABLE
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaLoader
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@IntegrationTest
class UnionContentMetaLoaderIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var unionContentMetaLoader: UnionContentMetaLoader

    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoTemplate

    @Test
    fun `load content meta`() = runBlocking<Unit> {
        val url = createRandomUrl()
        val contentMeta = createRandomContent()
        coEvery { testContentMetaLoader.fetchContentMeta(url) } returns contentMeta
        assertThat(unionContentMetaLoader.fetchContentMeta(url, randomEthItemId())).isEqualTo(contentMeta)
    }

    @Test
    fun `return content meta from cache`() = runBlocking<Unit> {
        val url = createRandomUrl()
        val contentMeta = createRandomContent()

        mongoTemplate.save(
            ContentMetaEntry(
                id = url,
                data = com.rarible.protocol.union.enrichment.meta.ContentMeta(
                    type = contentMeta.type,
                    width = contentMeta.width,
                    height = contentMeta.height,
                    size = contentMeta.size
                )
            ),
            CACHE_META_TABLE
        ).awaitFirstOrNull()

        assertThat(unionContentMetaLoader.fetchContentMeta(url, randomEthItemId())).isEqualTo(contentMeta)
        coVerify(exactly = 0) { testContentMetaLoader.fetchContentMeta(url) }
    }

    private fun createRandomUrl(): String =
        "https://image.com/${randomString()}"

    private fun createRandomContent(): ContentMeta = ContentMeta(
        type = randomString(),
        width = randomInt(),
        height = randomInt(),
        size = randomLong()
    )
}
