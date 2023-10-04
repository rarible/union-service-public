package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset

object ImxItemMetaConverter {

    private val logger by Logger()

    private val videoContentKeys = setOf("animation_url", "youtube_url")

    fun convert(asset: ImmutablexAsset, attributes: Set<String>?, blockchain: BlockchainDto): UnionMeta {
        return try {
            convertInternal(asset, attributes ?: emptySet())
        } catch (e: Exception) {
            logger.error("Failed to convert {} Meta: {} \n{}", blockchain, e.message, asset)
            throw e
        }
    }

    private fun convertInternal(asset: ImmutablexAsset, attributes: Set<String>): UnionMeta {
        val collectionName = asset.collection.name
        val assetName = asset.name ?: collectionName?.let { "$it #${asset.encodedTokenId()}" } ?: "Unknown"
        return UnionMeta(
            name = assetName,
            collectionId = asset.tokenAddress,
            description = asset.description,
            createdAt = asset.createdAt,
            content = getVideoContent(asset) + getImageContent(asset),
            attributes = asset.metadata?.filterKeys { it in attributes }?.map {
                UnionMetaAttribute(
                    key = it.key,
                    value = "${it.value}"
                )
            } ?: emptyList(),
            originalMetaUri = asset.uri,
            source = MetaSource.ORIGINAL,
        )
    }

    private fun getVideoContent(asset: ImmutablexAsset): List<UnionMetaContent> {
        return asset.metadata?.filterKeys { it in videoContentKeys }?.mapNotNull {
            val url = it.value as String? ?: return@mapNotNull null
            UnionMetaContent(
                url = url,
                // TODO we could get here duplicated ORIGINAL video
                representation = MetaContentDto.Representation.ORIGINAL,
                properties = UnionVideoProperties()
            )
        } ?: emptyList()
    }

    // Since there can be duplicates as image/image_url, it's safer to take root field imageUrl
    private fun getImageContent(asset: ImmutablexAsset): List<UnionMetaContent> {
        val imageUrl = asset.imageUrl ?: return emptyList()
        return listOf(
            UnionMetaContent(
                url = imageUrl,
                representation = MetaContentDto.Representation.ORIGINAL,
                properties = UnionImageProperties()
            )
        )
    }
}
