package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.AudioContentDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.HtmlContentDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.Model3dContentDto
import com.rarible.protocol.union.dto.VideoContentDto
import com.rarible.protocol.union.enrichment.download.DownloadEntry

object MetaDtoConverter {

    fun convert(meta: UnionCollectionMeta): CollectionMetaDto {
        return CollectionMetaDto(
            name = meta.name,
            description = meta.description,
            createdAt = meta.createdAt,
            tags = meta.tags,
            genres = meta.genres,
            language = meta.language,
            rights = meta.rights,
            rightsUri = meta.rightsUri,
            externalUri = meta.externalUri,
            originalMetaUri = meta.originalMetaUri,
            externalLink = meta.externalUri, // TODO remove later
            feeRecipient = meta.feeRecipient,
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
            originalMetaUri = meta.originalMetaUri,
            attributes = meta.attributes.map(::convert),
            content = meta.content.map { convert(it) },
        )
    }

    fun convert(metaEntry: DownloadEntry<UnionMeta>): MetaDto? {
        val meta = metaEntry.data ?: return null
        return MetaDto(
            name = meta.name,
            description = meta.description,
            createdAt = meta.createdAt,
            updatedAt = metaEntry.succeedAt,
            tags = meta.tags,
            genres = meta.genres,
            language = meta.language,
            rights = meta.rights,
            rightsUri = meta.rightsUri,
            externalUri = meta.externalUri,
            originalMetaUri = meta.originalMetaUri,
            attributes = meta.attributes.map(::convert),
            content = meta.content.map { convert(it) },
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
                width = properties.width,
                available = properties.available,
            )
            is UnionVideoProperties -> VideoContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width,
                available = properties.available,
            )
            is UnionAudioProperties -> AudioContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
                available = properties.available,
            )
            is UnionModel3dProperties -> Model3dContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
                available = properties.available,
            )
            is UnionHtmlProperties -> HtmlContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
                available = properties.available,
            )
            // Fallback: consider this was an image. It is better than to return nothing.
            else -> ImageContentDto(
                url = content.url,
                representation = MetaContentDto.Representation.ORIGINAL,
                mimeType = null,
                size = null,
                width = null,
                height = null,
                available = null,
            )
        }
    }

    fun convert(source: UnionMetaAttribute): MetaAttributeDto {
        return MetaAttributeDto(
            key = source.key,
            value = source.value,
            type = source.type,
            format = source.format,
        )
    }
}
