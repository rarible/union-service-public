package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.AudioContentDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.HtmlContentDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.Model3dContentDto
import com.rarible.protocol.union.dto.VideoContentDto

object EnrichedMetaConverter {
    fun convert(meta: UnionCollectionMeta): CollectionMetaDto {
        return CollectionMetaDto(
            description = meta.description,
            externalLink = meta.externalLink,
            feeRecipient = meta.feeRecipient,
            name = meta.name,
            sellerFeeBasisPoints = meta.sellerFeeBasisPoints,
            content = meta.content.map { convert(it) }
        )
    }

    fun convert(meta: UnionMeta): MetaDto {
        return MetaDto(
            name = meta.name,
            description = meta.description,
            createdAt = meta.createdAt,
            tags = meta.tags,
            genres = meta.genres,
            language = meta.language,
            rights = meta.rights,
            rightsUri = meta.rightsUri,
            externalUri = meta.externalUri,
            attributes = meta.attributes,
            content = meta.content.map { convert(it) },
            restrictions = meta.restrictions.map { it.type }.distinct()
        )
    }

    private fun convert(content: UnionMetaContent): MetaContentDto {
        return when (val properties = content.properties) {
            is UnionImageProperties -> ImageContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width
            )
            is UnionVideoProperties -> VideoContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width
            )
            is UnionAudioProperties -> AudioContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
            )
            is UnionModel3dProperties -> Model3dContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
            )
            is UnionHtmlProperties -> HtmlContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
            )
            // Fallback: consider this was an image. It is better than to return nothing.
            null -> ImageContentDto(
                url = content.url,
                representation = MetaContentDto.Representation.ORIGINAL,
                mimeType = null,
                size = null,
                width = null,
                height = null
            )
        }
    }
}
