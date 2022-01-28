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
    @Qualifier("content.meta.cache.loader.service")
    private val contentMetaCacheLoaderService: CacheLoaderService<ContentMeta>,
    private val ipfsUrlResolver: IpfsUrlResolver
) {

    suspend fun fetchContentMeta(
        url: String,
        timeout: Duration
    ): UnionMetaContentProperties? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        val contentMeta = contentMetaCacheLoaderService.getAvailableOrScheduleAndWait(
            key = realUrl,
            timeout = timeout
        ) ?: return null
        val isImage = contentMeta.type.contains("image")
        val isVideo = contentMeta.type.contains("video")
        val isAudio = contentMeta.type.contains("audio") // TODO: add dedicated properties for audio.
        return when {
            isImage -> contentMeta.toImageProperties()
            isVideo || isAudio -> contentMeta.toVideoProperties()
            else -> return null
        }
    }

    suspend fun refreshContentMeta(url: String) {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        contentMetaCacheLoaderService.update(realUrl)
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
