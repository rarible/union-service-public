package com.rarible.protocol.union.enrichment.meta.cache

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.enrichment.meta.content.cache.ContentCacheException
import com.rarible.protocol.union.enrichment.meta.content.cache.IpfsContentCache
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IpfsContentCacheTest {

    private val ipfsContentCache = IpfsContentCache(mockk())

    @Test
    fun `content is not full`() = runBlocking<Unit> {
        val resource = IpfsUrl("ipfs://abc", null, "abc")
        assertThrows<ContentCacheException> { ipfsContentCache.save(resource, UnionImageProperties()) }
    }

    @Test
    fun `unsupported resource`() = runBlocking<Unit> {
        val resource = HttpUrl("http://localhost:8080")
        assertThrows<ContentCacheException> {
            ipfsContentCache.save(resource, UnionAudioProperties("audio/mp3", 100))
        }
    }
}
