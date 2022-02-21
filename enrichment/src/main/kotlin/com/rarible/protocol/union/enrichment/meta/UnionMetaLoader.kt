package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
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
    private val unionContentMetaLoader: UnionContentMetaLoader,
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
        logger.info(
            "Resolving content meta for item ${itemId.fullId()} for URL ${content.url}" +
                if (resolvedUrl != content.url) " resolved as $resolvedUrl" else "",
        )
        /*
        // We should NOT re-use content properties received from blockchains since they are not support
        // model/audio types
        val knownContentProperties = content.properties
        if (knownContentProperties != null && !knownContentProperties.isEmpty()) {
            return content.copy(url = resolvedUrl)
        }
        */
        val fetchedContentMeta = unionContentMetaLoader.fetchContentMeta(resolvedUrl, itemId)
        val enrichedProperties = fetchedContentMeta?.toUnionMetaContentProperties()
        // Use at least some fields of the known properties - deprecated since audio/model has been introduced
        //?: knownContentProperties
            ?: UnionImageProperties() // Questionable, but let's consider that was an image.
        return content.copy(url = resolvedUrl, properties = enrichedProperties)
    }

    private fun ContentMeta.toUnionMetaContentProperties(): UnionMetaContentProperties? {
        val isImage = type.contains("image")
        val isVideo = type.contains("video")
        val isAudio = type.contains("audio") // TODO[media]: add dedicated properties for audio.
        val isModel = type.contains("model")
        return when {
            isImage -> toImageProperties()
            isVideo -> toVideoProperties()
            isAudio -> toAudioProperties()
            isModel -> toModel3dProperties()
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

    private fun ContentMeta.toAudioProperties() = UnionAudioProperties(
        mimeType = type,
        size = size
    )

    private fun ContentMeta.toModel3dProperties() = UnionModel3dProperties(
        mimeType = type,
        size = size
    )

    class UnionMetaResolutionException(message: String) : RuntimeException(message)
}
