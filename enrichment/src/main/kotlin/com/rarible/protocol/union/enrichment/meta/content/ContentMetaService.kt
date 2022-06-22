package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetector
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.EmbeddedContent
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.model.SchemaUrl
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentUrlProvider
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.plexus.util.StringUtils
import org.springframework.stereotype.Component

@Component
class ContentMetaService(
    private val urlParser: UrlParser,
    private val urlResolver: UrlResolver,
    private val embeddedContentDetector: EmbeddedContentDetector,
    private val embeddedContentUrlProvider: EmbeddedContentUrlProvider
) {

    fun detectEmbeddedContent(data: String): EmbeddedContent? {
        return embeddedContentDetector.detect(data)
    }

    fun parseUrl(url: String): UrlResource? {
        return urlParser.parse(url)
    }

    fun getEmbeddedSchemaUrl(id: String): String {
        return embeddedContentUrlProvider.getSchemaUrl(id)
    }

    fun getEmbeddedId(data: ByteArray): String {
        return DigestUtils.sha256Hex(data)
    }

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(url: String): String? {
        val resource = parseUrl(url) ?: return null
        return urlResolver.resolveInternalUrl(resource)
    }

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String): String? {
        val resource = parseUrl(url) ?: return null
        return urlResolver.resolvePublicUrl(resource)
    }

    fun resolveInternalHttpUrl(resource: UrlResource) = urlResolver.resolveInternalUrl(resource)

    fun resolvePublicHttpUrl(resource: UrlResource) = urlResolver.resolvePublicUrl(resource)

    fun resolveStorageHttpUrl(resource: UrlResource): String {
        return when (resource) {
            // IPFS urls without specified gateway will be stored as abstract URLs
            is IpfsUrl -> {
                if (StringUtils.isBlank(resource.originalGateway)) {
                    "ipfs://${resource.path}"
                } else {
                    resolvePublicHttpUrl(resource)
                }
            }
            // The same for custom schema urls
            is SchemaUrl -> resource.toSchemaUrl()
            // Other cases - as public urls
            else -> resolvePublicHttpUrl(resource)
        }
    }

    fun exposePublicUrls(meta: UnionMeta?, id: ItemIdDto): UnionMeta? {
        return meta?.let { it.copy(content = exposePublicUrls(it.content)) }
    }

    fun exposePublicUrls(meta: UnionCollectionMeta?, id: CollectionIdDto): UnionCollectionMeta? {
        return meta?.let { it.copy(content = exposePublicUrls(it.content)) }
    }

    private fun exposePublicUrls(content: List<UnionMetaContent>): List<UnionMetaContent> {
        return content.map {
            it.copy(url = exposePublicUrl(it.url))
        }
    }

    private fun exposePublicUrl(url: String): String {
        if (embeddedContentUrlProvider.isEmbeddedContentUrl(url)) {
            return embeddedContentUrlProvider.getPublicUrl(url)
        }
        return resolvePublicHttpUrl(url) ?: url
    }

    fun convertToProperties(content: ContentMeta): UnionMetaContentProperties? {
        val type = content.mimeType
        return when {
            type.contains("image") -> content.toImageProperties()
            type.contains("video") -> content.toVideoProperties()
            type.contains("audio") -> content.toAudioProperties()
            type.contains("model") -> content.toModel3dProperties()
            type.contains("text/html") -> content.toHtmlProperties()
            else -> return null
        }
    }

    private fun ContentMeta.toVideoProperties() = UnionVideoProperties(
        mimeType = mimeType,
        width = width,
        height = height,
        size = size
    )

    private fun ContentMeta.toImageProperties() = UnionImageProperties(
        mimeType = mimeType,
        width = width,
        height = height,
        size = size
    )

    private fun ContentMeta.toAudioProperties() = UnionAudioProperties(
        mimeType = mimeType,
        size = size
    )

    private fun ContentMeta.toModel3dProperties() = UnionModel3dProperties(
        mimeType = mimeType,
        size = size
    )

    private fun ContentMeta.toHtmlProperties() = UnionHtmlProperties(
        mimeType = mimeType,
        size = size
    )

}