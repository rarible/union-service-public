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
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.VideoContentDto
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
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

    suspend fun enrichMeta(meta: UnionMeta?, itemId: ShortItemId): MetaDto? {
        val metaToEnrich = meta ?: getMetaSafe(itemId.toDto())
        return metaToEnrich?.let { enrichMeta(it, itemId.toDto().fullId()) }
    }

    suspend fun enrichMeta(meta: UnionMeta, itemId: String): MetaDto {
        val enrichedContent = coroutineScope {
            meta.content.map { async { enrichContent(it, itemId) } }
        }.awaitAll()

        return MetaDto(
            name = meta.name,
            description = meta.description,
            attributes = meta.attributes,
            content = enrichedContent,
            restrictions = meta.restrictions.map { it.type }.distinct()
        )
    }

    suspend fun resetMeta(itemId: ItemIdDto) {
        val meta = getMetaSafe(itemId)
        meta?.let {
            meta.content.forEach { contentMetaService.resetContentMeta(it.url) }
        }
    }

    private suspend fun getMetaSafe(itemId: ItemIdDto): UnionMeta? {
        try {
            return router.getService(itemId.blockchain).getItemMetaById(itemId.value)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Raw meta for Item [{}] not found", itemId)
                return null
            } else {
                throw e
            }
        }
    }

    private suspend fun enrichContent(content: UnionMetaContent, itemId: String): MetaContentDto {
        val receivedProperties = content.properties

        val properties = if (receivedProperties == null) {
            // We know nothing about content - only URL
            val fetchedProperties = fetchMetaContentProperties(content.url, itemId)
            // If no metadata fetched - let it be an Image by default
            fetchedProperties ?: UnionImageProperties()
        } else if (receivedProperties.isEmpty()) {
            // Ok, we have some info about metadata, but it is not full - fetching it
            val fetchedProperties = fetchMetaContentProperties(content.url, itemId)
            // If fetched - good, otherwise using properties we have
            fetchedProperties ?: receivedProperties
        } else {
            // We have fully qualified meta - using it, request not required
            receivedProperties
        }

        return when (properties) {
            is UnionImageProperties -> {
                ImageContentDto(
                    url = content.url,
                    representation = content.representation,
                    mimeType = properties.mimeType,
                    height = properties.height,
                    size = properties.size,
                    width = properties.width
                )
            }
            is UnionVideoProperties -> {
                VideoContentDto(
                    url = content.url,
                    representation = content.representation,
                    mimeType = properties.mimeType,
                    height = properties.height,
                    size = properties.size,
                    width = properties.width
                )
            }
        }
    }

    private suspend fun fetchMetaContentProperties(url: String, itemId: String): UnionMetaContentProperties? {
        val contentMeta = contentMetaService.getContentMeta(url, itemId)
        val emptyMeta = createEmptyMetaProperties(contentMeta?.type)
        return when (emptyMeta) {
            is UnionImageProperties -> emptyMeta.copy(
                width = contentMeta?.width,
                height = contentMeta?.height,
                size = contentMeta?.size
            )
            is UnionVideoProperties -> emptyMeta.copy(
                width = contentMeta?.width,
                height = contentMeta?.height,
                size = contentMeta?.size
            )
            null -> null
        }
    }

    private fun createEmptyMetaProperties(mimeType: String?): UnionMetaContentProperties? {
        if (mimeType == null) {
            return null
        }
        if (mimeType.contains("image")) {
            return UnionImageProperties(mimeType)
        }
        if (mimeType.contains("video") || mimeType.contains("audio") || (mimeType.contains("model"))) {
            return UnionVideoProperties(mimeType)
        }
        return null
    }

}
