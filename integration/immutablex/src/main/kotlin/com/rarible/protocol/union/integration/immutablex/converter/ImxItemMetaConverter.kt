package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset

object ImxItemMetaConverter {

    private val logger by Logger()

    private val contentKeys = setOf("image_url", "image", "animation_url", "youtube_url")

    fun convert(asset: ImmutablexAsset, blockchain: BlockchainDto): UnionMeta {
        return try {
            convertInternal(asset)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Meta: {} \n{}", blockchain, e.message, asset)
            throw e
        }
    }

    private fun convertInternal(asset: ImmutablexAsset): UnionMeta {
        val collectionName = asset.collection.name
        val assetName = asset.name ?: collectionName?.let { "$it #${asset.encodedTokenId()}" } ?: "Unknown"
        return UnionMeta(
            name = assetName,
            description = asset.description,
            createdAt = asset.createdAt,
            content = asset.metadata?.filterKeys { it in contentKeys }?.map {
                UnionMetaContent(
                    url = it.value as String,
                    representation = MetaContentDto.Representation.ORIGINAL
                )
            } ?: emptyList(),
            attributes = asset.metadata?.filterKeys { it !in contentKeys }?.map {
                MetaAttributeDto(
                    key = it.key,
                    value = "${it.value}"
                )
            } ?: emptyList(),
            restrictions = emptyList()
        )
    }
}
