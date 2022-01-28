package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@CaptureSpan(type = SpanType.APP)
class ContentMetaService(
    private val metaProperties: MetaProperties,
    @Qualifier("content.meta.cache.loader.service")
    private val contentMetaCacheLoaderService: CacheLoaderService<ContentMeta>,
    private val ipfsUrlResolver: IpfsUrlResolver
) {

    suspend fun enrichWithContentMeta(content: UnionMetaContent): UnionMetaContent {
        val properties = content.properties
        val enrichedProperties = if (properties == null || properties.isEmpty()) {
            val fetchedProperties = fetchMetaContentProperties(content.url)
            fetchedProperties ?: properties ?: UnionImageProperties()
        } else {
            properties
        }
        return content.copy(properties = enrichedProperties)
    }

    suspend fun refreshContentMeta(content: UnionMetaContent) {
        val realUrl = ipfsUrlResolver.resolveRealUrl(content.url)
        contentMetaCacheLoaderService.update(realUrl)
    }

    private suspend fun getContentMeta(url: String): ContentMeta? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        return contentMetaCacheLoaderService.getAvailableOrScheduleAndWait(
            key = realUrl,
            timeout = Duration.ofMillis(metaProperties.timeoutLoadingContentMeta)
        )
    }

    private suspend fun fetchMetaContentProperties(url: String): UnionMetaContentProperties? {
        val contentMeta = getContentMeta(url) ?: return null
        val isImage = contentMeta.type.contains("image")
        val isVideo = contentMeta.type.contains("video")
        val isAudio = contentMeta.type.contains("audio") // TODO: add dedicated properties for audio.
        return when {
            isImage -> contentMeta.toImageProperties()
            isVideo || isAudio -> contentMeta.toVideoProperties()
            else -> return null
        }
    }

}

fun ContentMeta.toVideoProperties() = UnionVideoProperties(
    mimeType = type,
    width = width,
    height = height,
    size = size
)

fun ContentMeta.toImageProperties() = UnionImageProperties(
    mimeType = type,
    width = width,
    height = height,
    size = size
)
