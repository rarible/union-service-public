package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaContentDto
import java.time.ZoneOffset

object SimpleHashConverter {

    fun convert(source: SimpleHashItem): UnionMeta {
        return UnionMeta(
            name = source.name ?: "${source.collection?.name} #${source.tokenId}",
            collectionId = null,
            description = source.description ?: source.collection?.description,
            createdAt = source.createdDate?.toInstant(ZoneOffset.UTC),
            tags = emptyList(),
            genres = emptyList(),
            language = null,
            rights = null,
            rightsUri = null,
            externalUri = source.metadataOriginalUrl,
            originalMetaUri = source.metadataOriginalUrl,
            attributes = attributes(source),
            content = content(source),
            restrictions = emptyList()
        )
    }

    private fun content(source: SimpleHashItem): List<UnionMetaContent> {
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
                    representation = MetaContentDto.Representation.ORIGINAL
                )
            }
        )
    }

    private fun attributes(source: SimpleHashItem): List<UnionMetaAttribute> {
        return source.extraMetadata?.attributes?.map {
            UnionMetaAttribute(key = it.traitType, value = it.value)
        } ?: emptyList()
    }

}
