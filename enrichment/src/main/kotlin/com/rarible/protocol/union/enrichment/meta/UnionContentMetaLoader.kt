package com.rarible.protocol.union.enrichment.meta

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
class UnionContentMetaLoader(
    private val unionContentMetaProvider: UnionContentMetaProvider,
    private val unionContentMetaService: UnionContentMetaService,
    private val embeddedContentService: EmbeddedContentService,
    private val metrics: UnionMetaMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichContent(
        itemId: ItemIdDto,
        metaContent: List<UnionMetaContent>
    ): List<UnionMetaContent> = coroutineScope {
        metaContent.map { content ->
            async {
                // Checking if there is embedded content first
                val embedded = unionContentMetaService.detectEmbeddedContent(content.url)
                embedded?.let {
                    return@async embedContent(itemId, content, it)
                }

                // Now check is there is valid url
                val resource = unionContentMetaService.parseUrl(content.url)
                if (resource == null) {
                    metrics.onContentResolutionFailed(itemId.blockchain, "remote", "unknown_url_format")
                    logger.warn("Unknown URL format: ${content.url}")
                    return@async content
                }

                downloadContent(itemId, content, resource)
            }
        }.awaitAll()
    }

    private suspend fun downloadContent(
        itemId: ItemIdDto,
        content: UnionMetaContent,
        resource: UrlResource
    ): UnionMetaContent {

        val resolvedProperties = unionContentMetaProvider.getContent(itemId, resource)
        val internalUrl = unionContentMetaService.resolveInternalHttpUrl(resource)

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
            url = unionContentMetaService.resolveStorageHttpUrl(resource),
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
        val converted = unionContentMetaService.convertToProperties(contentMeta)
        when (converted) {
            null -> metrics.onContentResolutionFailed(blockchain, "embedded", "unknown_mime_type")
            else -> metrics.onContentFetched(blockchain, "embedded", converted)
        }

        val properties = converted
            ?: original.properties
            ?: UnionImageProperties() // The same logic as for remote meta - we can't determine type, image by default

        val toSave = UnionEmbeddedContent(
            id = unionContentMetaService.getEmbeddedId(embedded.content),
            mimeType = properties.mimeType ?: embedded.meta.mimeType,
            size = embedded.content.size,
            data = embedded.content
        )

        embeddedContentService.save(toSave)

        logger.info("Resolved embedded meta content ${original.representation}: $properties")
        return original.copy(
            url = unionContentMetaService.getEmbeddedSchemaUrl(toSave.id),
            properties = properties
        )
    }
}