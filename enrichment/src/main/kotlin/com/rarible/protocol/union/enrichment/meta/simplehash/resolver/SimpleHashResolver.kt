package com.rarible.protocol.union.enrichment.meta.simplehash.resolver

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem
import java.time.ZoneOffset

open class SimpleHashResolver(
    private val mapper: ObjectMapper
) {

    open fun support(source: SimpleHashItem): Boolean {
        return true
    }

    open fun convert(source: SimpleHashItem): UnionMeta {
        return UnionMeta(
            name = source.name ?: "${source.collection?.name} #${SimpleHashConverter.safeParseTokenId(source.nftId)}",
            collectionId = null,
            description = source.description ?: source.collection?.description,
            createdAt = source.createdDate?.toInstant(ZoneOffset.UTC),
            tags = emptyList(),
            genres = emptyList(),
            language = null,
            rights = null,
            rightsUri = null,
            externalUri = source.externalUrl,
            originalMetaUri = source.extraMetadata?.metadataOriginalUrl,
            attributes = attributes(source),
            content = content(source),
            restrictions = emptyList(),
            source = MetaSource.SIMPLE_HASH,
        )
    }

    fun content(source: SimpleHashItem): List<UnionMetaContent> {
        return listOfNotNull(
            source.previews?.imageLargeUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.BIG
                )
            },
            source.previews?.imageOpengraphUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.PORTRAIT
                )
            },
            source.previews?.imageSmallUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.PREVIEW
                )
            },
            source.extraMetadata?.imageOriginalUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.ORIGINAL,
                    // Supposed that SimpleHash item meta property "image_properties" applies to "extra_metadata.image_original_url"
                    properties = source.imageProperties?.let { properties ->
                        UnionImageProperties(
                            size = properties.size,
                            width = properties.width,
                            height = properties.height,
                            mimeType = properties.mimeType
                        )
                    }
                )
            }
        )
    }

    open fun attributes(source: SimpleHashItem): List<UnionMetaAttribute> {
        return source.extraMetadata?.attributes?.map {
            UnionMetaAttribute(key = it.traitType, value = it.value)
        } ?: emptyList()
    }

    fun address(source: String): String {
        val parts = source.split(".")
        return when (parts.size) {
            0 -> source
            else -> parts[1]
        }
    }
}
