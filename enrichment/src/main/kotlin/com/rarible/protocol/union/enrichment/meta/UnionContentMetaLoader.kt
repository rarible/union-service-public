package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.ItemIdDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionContentMetaLoader(
    private val contentMetaReceiver: ContentMetaReceiver
) {
    private val logger = LoggerFactory.getLogger(UnionContentMetaLoader::class.java)

    suspend fun fetchContentMeta(url: String, itemId: ItemIdDto): UnionMetaContentProperties? {
        val logPrefix = "Content meta resolution for ${itemId.fullId()} by $url"
        logger.info("$logPrefix: starting to resolve")
        val contentMeta = try {
            contentMetaReceiver.receive(url)
        } catch (e: Exception) {
            logger.warn("$logPrefix: error: ${e.message}", itemId.fullId(), url, e)
            null
        } ?: return null
        val contentProperties = contentMeta.toUnionMetaContentProperties()
        logger.info("$logPrefix: resolved $contentProperties")
        return contentProperties
    }

    private fun ContentMeta.toUnionMetaContentProperties(): UnionMetaContentProperties? {
        val isImage = type.contains("image")
        val isVideo = type.contains("video")
        val isAudio = type.contains("audio") // TODO[media]: add dedicated properties for audio.
        val isModel = type.contains("model")
        val isHtml = type.contains("text/html")
        return when {
            isImage -> toImageProperties()
            isVideo -> toVideoProperties()
            isAudio -> toAudioProperties()
            isModel -> toModel3dProperties()
            isHtml -> toHtmlProperties()
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

    private fun ContentMeta.toHtmlProperties() = UnionHtmlProperties(
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
