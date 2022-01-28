package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContentMetaCacheLoader(
    private val contentMetaLoader: ContentMetaLoader
) : CacheLoader<ContentMeta> {

    private val logger = LoggerFactory.getLogger(ContentMetaCacheLoader::class.java)

    override val type get() = TYPE

    override suspend fun load(key: String): ContentMeta {
        logger.info("Resolving content meta for '$key'")
        return try {
            contentMetaLoader.fetchContentMeta(key)
        } catch (e: Exception) {
            throw ContentMetaLoaderException("Failed to load content meta for '$key'", e)
        } ?: throw ContentMetaLoaderException("No content meta resolved for '$key'")
    }

    class ContentMetaLoaderException : RuntimeException {
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(message: String) : super(message)
    }

    companion object {
        const val TYPE: CacheType = "content-meta"
    }
}
