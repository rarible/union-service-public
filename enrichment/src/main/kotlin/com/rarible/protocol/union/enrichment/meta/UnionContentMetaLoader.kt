package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component

@Component
class UnionContentMetaLoader(
    private val contentMetaReceiver: ContentMetaReceiver,
    private val template: ReactiveMongoTemplate
) {
    private val logger = LoggerFactory.getLogger(UnionContentMetaLoader::class.java)

    suspend fun fetchContentMeta(url: String, itemId: ItemIdDto): UnionMetaContentProperties? {
        val logPrefix = "Content meta resolution for ${itemId.fullId()} by $url"
        logger.info("$logPrefix: starting to resolve")
        val fromCache = fetchFromCache(url)
        if (fromCache != null) {
            val properties = fromCache.toUnionMetaContentProperties()
            logger.info("$logPrefix: found in the cache: $properties")
            return properties
        }
        val contentMeta = try {
            contentMetaReceiver.receive(url)
        } catch (e: Exception) {
            logger.warn("$logPrefix: error", itemId.fullId(), url, e)
            null
        } ?: return null
        val contentProperties = contentMeta.toUnionMetaContentProperties()
        logger.info("$logPrefix: resolved $contentProperties")
        return contentProperties
    }

    private suspend fun fetchFromCache(url: String): ContentMeta? {
        for (candidateUrl in getCandidateUrls(url)) {
            val cacheEntry = template.findById<CachedContentMetaEntry>(
                id = candidateUrl,
                collectionName = CachedContentMetaEntry.CACHE_META_COLLECTION
            ).awaitFirstOrNull()
            if (cacheEntry != null) {
                return cacheEntry.data.let {
                    ContentMeta(
                        type = it.type,
                        width = it.width,
                        height = it.height,
                        size = it.size
                    )
                }
            }
        }
        return null
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun getCandidateUrls(url: String): List<String> =
        buildList {
            add(url)
            val hash = getIpfsHash(url)
            if (hash != null) {
                ipfsPrefixes.mapTo(this) { it + hash }
            }
        }.distinct()

    private fun getIpfsHash(url: String): String? {
        for (prefix in ipfsPrefixes) {
            if (url.startsWith(prefix)) {
                return url.substringAfter(prefix)
            }
        }
        return null
    }

    companion object {
        private const val rariblePinata = "https://rarible.mypinata.cloud/ipfs/"
        private const val ipfsRarible = "https://ipfs.rarible.com/ipfs/"
        private val ipfsPrefixes = listOf(rariblePinata, ipfsRarible)
    }

}
