package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.core.meta.resource.model.EmbeddedContent
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import com.rarible.protocol.union.enrichment.meta.embedded.UnionEmbeddedContent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContentMetaDownloader(
    private val contentMetaProvider: ContentMetaProvider,
    private val contentMetaService: ContentMetaService,
    private val embeddedContentService: EmbeddedContentService,
    private val metrics: ContentMetaMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichContent(
        itemId: ItemIdDto,
        metaContent: List<UnionMetaContent>
    ): List<UnionMetaContent> = coroutineScope {
        metaContent.map { content ->
            async {
                // Checking if there is embedded content first
                val embedded = contentMetaService.detectEmbeddedContent(content.url)
                embedded?.let {
                    return@async embedContent(itemId, content, it)
                }

                // Now check is there is valid url
                val resource = contentMetaService.parseUrl(content.url)
                if (resource == null) {
                    metrics.onContentResolutionFailed(itemId.blockchain, "remote", "unknown_url_format")
                    logger.warn("Unknown URL format in content of Item $itemId: ${content.url}")
                    return@async null
                }

                downloadContent(itemId, content, resource)
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun downloadContent(
        itemId: ItemIdDto,
        content: UnionMetaContent,
        resource: UrlResource
    ): UnionMetaContent {

        val resolvedProperties = contentMetaProvider.getContent(itemId, resource)
        val internalUrl = contentMetaService.resolveInternalHttpUrl(resource)

        val contentProperties = when {
            resolvedProperties != null -> {
                logger.info("Content meta from $internalUrl resolved to $resolvedProperties")
                resolvedProperties
            }
            content.properties != null -> {
                logger.info("Content meta from $internalUrl is not resolved, using ${content.properties}")
                content.properties
            }
            else -> {
                logger.warn("Content meta from $internalUrl is not resolved, content metadata is unknown")
                UnionUnknownProperties()
            }
        }

        return content.copy(
            url = contentMetaService.resolveStorageHttpUrl(resource),
            properties = contentProperties
        )
    }

    private suspend fun embedContent(
        itemId: ItemIdDto,
        original: UnionMetaContent,
        embedded: EmbeddedContent
    ): UnionMetaContent {
        val blockchain = itemId.blockchain
        val contentMeta = embedded.meta
        val converted = contentMetaService.convertToProperties(contentMeta)
        when (converted) {
            null -> metrics.onContentResolutionFailed(blockchain, "embedded", "unknown_mime_type")
            else -> metrics.onContentFetched(blockchain, "embedded", converted)
        }

        val properties = converted
            ?: original.properties
            ?: UnionImageProperties() // The same logic as for remote meta - we can't determine type, image by default

        val toSave = UnionEmbeddedContent(
            id = contentMetaService.getEmbeddedId(embedded.content),
            mimeType = properties.mimeType ?: embedded.meta.mimeType,
            available = true,
            size = embedded.content.size,
            data = embedded.content
        )

        embeddedContentService.save(toSave)

        logger.info("Resolved embedded meta content ${original.representation}: $properties")
        return original.copy(
            url = contentMetaService.getEmbeddedSchemaUrl(toSave.id),
            properties = properties.withAvailable(true)
        )
    }
}