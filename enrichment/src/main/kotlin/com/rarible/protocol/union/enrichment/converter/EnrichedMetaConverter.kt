package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.VideoContentDto

object EnrichedMetaConverter {
    fun convert(meta: UnionMeta): MetaDto {
        return MetaDto(
            name = meta.name,
            description = meta.description,
            attributes = meta.attributes,
            content = meta.content.mapNotNull { convert(it) },
            restrictions = meta.restrictions.map { it.type }.distinct()
        )
    }

    private fun convert(content: UnionMetaContent): MetaContentDto? {
        return when (val properties = content.properties) {
            is UnionImageProperties -> ImageContentDto(
                url = content.url,
                representation = content.representation,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width
            )
            is UnionVideoProperties -> VideoContentDto(
                url = content.url,
                representation = content.representation,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width
            )
            null -> null
        }
    }
}
