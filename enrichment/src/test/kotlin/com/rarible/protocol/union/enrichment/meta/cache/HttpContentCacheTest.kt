package com.rarible.protocol.union.enrichment.meta.cache

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.enrichment.meta.content.cache.ContentCacheStorage
import com.rarible.protocol.union.enrichment.meta.content.cache.HttpContentCache
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HttpContentCacheTest {

    private val contentCacheStorage = mockk<ContentCacheStorage>(relaxed = true)
    private val universalContentCache = HttpContentCache(contentCacheStorage)

    @Test
    fun `save resource`() = runBlocking {
        val resource = HttpUrl("http://localhost:8080")

        val content = UnionAudioProperties("audio/mp3", 100)
        val result = universalContentCache.save(resource, content)

        coVerify { contentCacheStorage.save(any()) }

        Assertions.assertEquals(content, result.content)
        Assertions.assertEquals("http://localhost:8080", result.url)
    }

}
