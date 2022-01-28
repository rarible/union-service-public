package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class ContentMetaService(
    private val mediaMetaService: MediaMetaService,
    private val ipfsUrlResolver: IpfsUrlResolver
) {

    suspend fun enrichContent(content: UnionMetaContent, itemId: ItemIdDto): UnionMetaContent {
        val properties = content.properties
        val enrichedProperties = if (properties == null || properties.isEmpty()) {
            val fetchedProperties = fetchMetaContentProperties(content.url)
            fetchedProperties ?: properties ?: UnionImageProperties()
        } else {
            properties
        }
        return content.copy(properties = enrichedProperties)
    }

    suspend fun getContentMeta(url: String): ContentMeta? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        return mediaMetaService.get(realUrl).awaitFirstOrNull()
    }

    private suspend fun fetchMetaContentProperties(url: String): UnionMetaContentProperties? {
        val contentMeta = getContentMeta(url) ?: return null
        val isImage = contentMeta.type.contains("image")
        val isVideo = contentMeta.type.contains("video")
        val isAudio = contentMeta.type.contains("audio") // TODO: add dedicated properties for audio.
        return when {
            isImage -> UnionImageProperties(
                mimeType = contentMeta.type,
                width = contentMeta.width,
                height = contentMeta.height,
                size = contentMeta.size
            )
            isVideo || isAudio -> UnionVideoProperties(
                mimeType = contentMeta.type,
                width = contentMeta.width,
                height = contentMeta.height,
                size = contentMeta.size
            )
            else -> return null
        }
    }
}
