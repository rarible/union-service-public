package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.meta.resource.model.EmbeddedContent
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionUnknownProperties
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
    private val embeddedContentService: EmbeddedContentService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichContentMeta(
        metaContent: List<UnionMetaContent>
    ): List<UnionMetaContent> = coroutineScope {
        metaContent.map { content ->
            async {
                val embedded = unionContentMetaService.detectEmbeddedContent(content.url)
                embedded?.let {
                    return@async embedMetaContent(content, it)
                }

                val resource = unionContentMetaService.parseUrl(content.url)
                if (resource == null) {
                    logger.info("Unknown URL format - ${content.url}")
                    return@async content
                }

                downloadMetaContent(content, resource)
            }
        }.awaitAll()
    }

    private suspend fun downloadMetaContent(content: UnionMetaContent, resource: UrlResource): UnionMetaContent {

        val resolvedProperties = unionContentMetaProvider.getContentMeta(resource)
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

    private suspend fun embedMetaContent(
        original: UnionMetaContent,
        embedded: EmbeddedContent
    ): UnionMetaContent {

        val contentMeta = embedded.meta
        val properties = unionContentMetaService.convertToProperties(contentMeta)
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