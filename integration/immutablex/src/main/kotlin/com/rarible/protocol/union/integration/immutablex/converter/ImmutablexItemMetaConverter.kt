package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset


object ImmutablexItemMetaConverter {

    private val logger by Logger()

    private val contentKeys = setOf("image_url", "image", "animation_url", "youtube_url")

    fun convert(asset: ImmutablexAsset): UnionMeta {
        return try {
            convertInternal(asset)
        } catch (e: Exception) {
            logger.error("Convert item meta failed! ${e.message}", e)
            throw e
        }
    }

    private fun convertInternal(asset: ImmutablexAsset): UnionMeta {
        return UnionMeta(
            name = asset.name!!,
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
