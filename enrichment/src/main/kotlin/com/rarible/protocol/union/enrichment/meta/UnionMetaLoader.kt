package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class UnionMetaLoader(
    private val router: BlockchainRouter<ItemService>,
    private val contentMetaLoader: ContentMetaLoader,
    private val ipfsUrlResolver: IpfsUrlResolver
) {
    private val logger = LoggerFactory.getLogger(UnionMetaLoader::class.java)

    suspend fun load(itemId: ItemIdDto): UnionMeta {
        val unionMeta = getItemMeta(itemId) ?: throw UnionMetaResolutionException("Cannot resolve meta for $itemId")
        return enrichContentMeta(unionMeta, itemId)
    }

    private suspend fun getItemMeta(itemId: ItemIdDto): UnionMeta? {
        return try {
            router.getService(itemId.blockchain).getItemMetaById(itemId.value)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }
    }

    private suspend fun enrichContentMeta(meta: UnionMeta, itemId: ItemIdDto): UnionMeta {
        return meta.copy(content = coroutineScope {
            meta.content.map { async { enrichContentMeta(it, itemId) } }.awaitAll()
        })
    }

    private suspend fun enrichContentMeta(
        content: UnionMetaContent,
        itemId: ItemIdDto
    ): UnionMetaContent {
        val resolvedUrl = ipfsUrlResolver.resolveRealUrl(content.url)
        if (content.url != resolvedUrl) {
            logger.info(
                "Content meta resolution: content URL of item {} was resolved from {} to {}",
                itemId.fullId(), content.url, resolvedUrl
            )
        }
        val knownContentProperties = content.properties
        if (knownContentProperties != null && !knownContentProperties.isEmpty()) {
            return content.copy(url = resolvedUrl)
        }
        val fetchedContentMeta = fetchContentMeta(resolvedUrl, itemId)
        val enrichedProperties = fetchedContentMeta?.toUnionMetaContentProperties()
            ?: knownContentProperties // Use at least some fields of the known properties.
            ?: UnionImageProperties() // Questionable, but let's consider that was an image.
        return content.copy(url = resolvedUrl, properties = enrichedProperties)
    }

    private fun ContentMeta.toUnionMetaContentProperties(): UnionMetaContentProperties? {
        val isImage = type.contains("image")
        val isVideo = type.contains("video")
        val isAudio = type.contains("audio") // TODO[media]: add dedicated properties for audio.
        return when {
            isImage -> toImageProperties()
            isVideo || isAudio -> toVideoProperties()
            else -> return null
        }
    }

    private fun ContentMeta.toVideoProperties() = UnionVideoProperties(
        mimeType = type,
        width = width,
        height = height,
        size = size
    )

    private fun ContentMeta.toImageProperties() = UnionImageProperties(
        mimeType = type,
        width = width,
        height = height,
        size = size
    )

    private suspend fun fetchContentMeta(url: String, itemId: ItemIdDto): ContentMeta? {
        val contentMeta = try {
            contentMetaLoader.fetchContentMeta(url)
        } catch (e: Exception) {
            logger.warn("Content meta resolution: error for {} by URL {}", itemId.fullId(), url, e)
            return null
        }
        if (contentMeta == null) {
            logger.warn("Content meta resolution: nothing was resolved for {} by URL {}", itemId.fullId(), url)
        }
        return contentMeta
    }

    class UnionMetaResolutionException(message: String) : RuntimeException(message)
}
