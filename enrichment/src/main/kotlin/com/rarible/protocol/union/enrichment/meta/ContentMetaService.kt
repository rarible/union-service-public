package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.withTimeoutOrNull
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

    suspend fun fetchContentMetaWithTimeout(
        url: String,
        timeout: Duration
    ): UnionMetaContentProperties? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        val contentMeta = getAvailableContentMetaOrWaitFetching(realUrl, timeout) ?: return null
        val isImage = contentMeta.type.contains("image")
        val isVideo = contentMeta.type.contains("video")
        val isAudio = contentMeta.type.contains("audio") // TODO: add dedicated properties for audio.
        return when {
            isImage -> contentMeta.toImageProperties()
            isVideo || isAudio -> contentMeta.toVideoProperties()
            else -> return null
        }
    }

    private suspend fun getAvailableContentMetaOrWaitFetching(
        url: String,
        timeout: Duration
    ): ContentMeta? {
        val availableMeta = contentMetaCacheLoaderService.getAvailable(url)
        if (availableMeta != null) {
            return availableMeta
        }
        if (isMetaInitiallyLoadedOrFailed(url)) {
            return null
        }
        contentMetaCacheLoaderService.update(url)
        return withTimeoutOrNull(timeout) {
            while (isActive) {
                if (isMetaInitiallyLoadedOrFailed(url)) {
                    return@withTimeoutOrNull contentMetaCacheLoaderService.getAvailable(url)
                }
                delay(100)
            }
            return@withTimeoutOrNull null
        }
    }

    /**
     * Returns `true` if the content meta by URL has been loaded or loading has failed,
     * and `false` if we haven't requested the meta loading or haven't received any result yet.
     */
    suspend fun isMetaInitiallyLoadedOrFailed(url: String): Boolean =
        when (val cacheEntry = contentMetaCacheLoaderService.get(url)) {
            is CacheEntry.Loaded -> true
            is CacheEntry.LoadedAndUpdateScheduled -> true
            is CacheEntry.LoadedAndUpdateFailed -> true
            is CacheEntry.InitialLoadScheduled -> when (cacheEntry.loadStatus) {
                is LoadTaskStatus.Scheduled -> false
                is LoadTaskStatus.WaitsForRetry -> true
            }
            is CacheEntry.InitialFailed -> true
            is CacheEntry.NotAvailable -> false
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
