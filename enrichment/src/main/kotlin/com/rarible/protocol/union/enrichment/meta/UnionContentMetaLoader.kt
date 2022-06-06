package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.meta.resource.UrlResource
import com.rarible.core.meta.resource.model.EmbeddedContent
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import com.rarible.protocol.union.enrichment.meta.embedded.UnionEmbeddedContent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionContentMetaLoader(
    private val contentMetaReceiver: ContentMetaReceiver,
    private val unionContentMetaService: UnionContentMetaService,
    private val embeddedContentService: EmbeddedContentService
) {

    private val logger = LoggerFactory.getLogger(UnionMetaLoader::class.java)

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

        val internalUrl = unionContentMetaService.resolveInternalHttpUrl(resource)
        if (internalUrl == content.url) {
            logger.info("Fetching content meta by URL $internalUrl")
        } else {
            logger.info("Fetching content meta by URL $internalUrl (original URL is ${content.url})")
        }

        val resolvedContentMeta = try {
            contentMetaReceiver.receive(internalUrl)
        } catch (e: Exception) {
            logger.warn("Failed to receive content meta via URL {}", internalUrl, e)
            null
        }

        val contentProperties = when {
            resolvedContentMeta != null -> {
                logger.info("Content meta from $internalUrl resolved to $resolvedContentMeta")
                unionContentMetaService.convertToProperties(resolvedContentMeta)
            }
            content.properties != null -> {
                logger.info("Content meta from $internalUrl is not resolved, using ${content.properties}")
                content.properties
            }
            else -> {
                logger.warn("Content meta from $internalUrl is not resolved, considered as image")
                UnionImageProperties()
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