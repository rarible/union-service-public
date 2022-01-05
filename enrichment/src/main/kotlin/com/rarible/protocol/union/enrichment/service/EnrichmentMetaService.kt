package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentMetaService(
    private val router: BlockchainRouter<ItemService>,
    private val contentMetaService: ContentMetaService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichMeta(itemId: ItemIdDto, originalMetaHint: UnionMeta?): UnionMeta? {
        val meta = originalMetaHint ?: getMetaSafeIfNotFound(itemId) ?: return null
        val enrichedContent = coroutineScope {
            meta.content.map { async { enrichContent(it, itemId) } }
        }.awaitAll()
        return meta.copy(content = enrichedContent)
    }

    suspend fun resetMeta(itemId: ItemIdDto) {
        // TODO[meta-3.0]: re-implement to not request meta here. Record to database with [itemId] and delete by this key.
        val meta = getMetaSafeIfNotFound(itemId)
        meta?.let {
            meta.content.forEach { contentMetaService.resetContentMeta(it.url) }
        }
    }

    private suspend fun getMetaSafeIfNotFound(itemId: ItemIdDto): UnionMeta? {
        return try {
            router.getService(itemId.blockchain).getItemMetaById(itemId.value)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Raw meta for Item [{}] not found", itemId)
                null
            } else {
                throw e
            }
        }
    }

    private suspend fun enrichContent(content: UnionMetaContent, itemId: ItemIdDto): UnionMetaContent {
        val properties = content.properties
        val enrichedProperties = if (properties == null || properties.isEmpty()) {
            val fetchedProperties = fetchMetaContentProperties(content.url, itemId)
            fetchedProperties ?: properties ?: UnionImageProperties()
        } else {
            properties
        }
        return content.copy(properties = enrichedProperties)
    }

    private suspend fun fetchMetaContentProperties(url: String, itemId: ItemIdDto): UnionMetaContentProperties? {
        val contentMeta = contentMetaService.getContentMeta(url, itemId) ?: return null
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
