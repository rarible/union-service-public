package com.rarible.protocol.union.listener.meta

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaProvider
import com.rarible.protocol.union.enrichment.meta.cache.ContentCacheStorage
import com.rarible.protocol.union.enrichment.meta.cache.UnionContentCacheEntry
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URL

@IntegrationTest
class UnionContentMetaProviderIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var unionContentMetaProvider: UnionContentMetaProvider

    @Autowired
    lateinit var contentCacheStorage: ContentCacheStorage

    @Autowired
    lateinit var urlParser: UrlParser

    private val cid = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"

    private val itemId = randomEthItemId()

    @Test
    fun `cacheable url - cached`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!
        // Fully qualified content, should be cached
        val contentMeta = ContentMeta(MimeType.PNG_IMAGE.value, 100, 100, 100)

        coEvery { testContentMetaReceiver.receive(any<URL>()) } returns contentMeta

        val properties = unionContentMetaProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")!!

        // Content returned and cached
        assertThat(properties).isEqualTo(fromCache.content)
    }

    @Test
    fun `cacheable url - not cached, content is not full`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("ipfs://$path")!!
        // Partially qualified content, should not be cached
        val contentMeta = ContentMeta(MimeType.PNG_IMAGE.value)

        coEvery { testContentMetaReceiver.receive(any<String>()) } returns contentMeta

        unionContentMetaProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `cacheable url - not cached, content is null`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("ipfs://$path")!!

        // Content not resolved
        coEvery { testContentMetaReceiver.receive(any<String>()) } returns null

        unionContentMetaProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `cacheable url - from cache`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse(path)!!

        val entry = UnionContentCacheEntry(
            url = "ipfs://$path",
            type = "ipfs",
            updatedAt = nowMillis(),
            content = UnionImageProperties(MimeType.PNG_IMAGE.value)
        )

        contentCacheStorage.save(entry)

        val properties = unionContentMetaProvider.getContent(itemId, urlResource)

        // Content returned and cached
        assertThat(properties).isEqualTo(entry.content)
    }

    @Test
    fun `not cacheable url`() = runBlocking<Unit> {
        val urlResource = urlParser.parse("https://localhost:8080/abc")!!
        val contentMeta = ContentMeta(MimeType.PNG_IMAGE.value)

        coEvery { testContentMetaReceiver.receive(any<String>()) } returns contentMeta

        unionContentMetaProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get(urlResource.original)

        // Not cached
        assertThat(fromCache).isNull()
    }

}