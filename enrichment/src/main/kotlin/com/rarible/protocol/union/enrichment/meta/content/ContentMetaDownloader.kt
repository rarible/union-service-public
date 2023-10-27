package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.core.common.asyncWithTraceId
import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.model.EmbeddedContent
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import com.rarible.protocol.union.enrichment.meta.embedded.UnionEmbeddedContent
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ContentMetaDownloader(
    private val contentMetaProvider: ContentMetaProvider,
    private val contentMetaService: ContentMetaService,
    private val embeddedContentService: EmbeddedContentService,
    private val metrics: ContentMetaMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichContent(
        id: String,
        blockchain: BlockchainDto,
        metaContent: List<UnionMetaContent>
    ): List<UnionMetaContent> = coroutineScope {
        metaContent.map { content ->
            asyncWithTraceId {
                // Checking if there is embedded content first
                val start = nowMillis()
                val embedded = contentMetaService.detectEmbeddedContent(content.url)
                embedded?.let {
                    return@asyncWithTraceId embedContent(id, blockchain, start, content, it)
                }

                // Now check is there is valid url
                val resource = contentMetaService.parseUrl(content.url)
                if (resource == null) {
                    metrics.onContentResolutionFailed(
                        blockchain = blockchain,
                        start = start,
                        source = ContentMetaProvider.SOURCE,
                        approach = "unknown",
                        reason = "malformed_url",
                    )
                    logger.warn("Unknown URL format in content of $id: ${content.url}")
                    return@asyncWithTraceId null
                }

                downloadContent(
                    id = id,
                    blockchain = blockchain,
                    start = start,
                    content = content,
                    resource = resource
                )
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun downloadContent(
        id: String,
        blockchain: BlockchainDto,
        start: Instant,
        content: UnionMetaContent,
        resource: UrlResource
    ): UnionMetaContent {

        val resolvedProperties = contentMetaProvider.getContent(id, blockchain, start, resource)
        val internalUrl = contentMetaService.resolveInternalHttpUrl(resource)

        val contentProperties = when {
            resolvedProperties != null -> {
                logger.info("Content meta for $id from $internalUrl resolved to $resolvedProperties")
                resolvedProperties
            }
            content.properties != null -> {
                logger.info("Content meta for $id from $internalUrl is not resolved, using ${content.properties}")
                content.properties
            }
            else -> {
                logger.warn("Content meta for $id from $internalUrl is not resolved, content metadata is unknown")
                UnionUnknownProperties()
            }
        }

        return content.copy(
            url = contentMetaService.resolveStorageHttpUrl(resource),
            properties = contentProperties
        )
    }

    private suspend fun embedContent(
        id: String,
        blockchain: BlockchainDto,
        start: Instant,
        original: UnionMetaContent,
        embedded: EmbeddedContent
    ): UnionMetaContent {
        val contentMeta = embedded.meta
        val converted = contentMetaService.convertToProperties(contentMeta)
        when (converted) {
            null -> metrics.onContentResolutionFailed(
                blockchain = blockchain,
                start = start,
                source = "embedded",
                approach = "unknown",
                reason = "unknown_mime_type",
            )

            else -> metrics.onContentFetched(
                blockchain = blockchain,
                start = start,
                source = "embedded",
                approach = "exif",
                properties = converted,
            )
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

        logger.info("Resolved embedded meta content for $id (${original.representation}): $properties")
        return original.copy(
            url = contentMetaService.getEmbeddedSchemaUrl(toSave.id),
            properties = properties.withAvailable(true)
        )
    }
}
