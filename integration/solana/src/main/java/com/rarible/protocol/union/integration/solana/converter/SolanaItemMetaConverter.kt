package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.TokenMetaAttributeDto
import com.rarible.protocol.solana.dto.TokenMetaContentDto
import com.rarible.protocol.solana.dto.TokenMetaDto
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaContentDto

object SolanaItemMetaConverter {

    fun convert(tokenMeta: TokenMetaDto): UnionMeta =
        UnionMeta(
            collectionId = tokenMeta.collectionId,
            name = tokenMeta.name,
            description = tokenMeta.description,
            attributes = tokenMeta.attributes.map { convert(it) },
            content = tokenMeta.content.map { convert(it) },
            restrictions = emptyList()
        )

    fun convert(source: TokenMetaAttributeDto): UnionMetaAttribute {
        return UnionMetaAttribute(
            key = source.key,
            value = source.value,
            type = source.type,
            format = source.format
        )
    }

    fun convert(source: TokenMetaContentDto): UnionMetaContent {
        return UnionMetaContent(
            url = source.url,
            representation = when (source.representation) {
                TokenMetaContentDto.Representation.PREVIEW -> MetaContentDto.Representation.PREVIEW
                TokenMetaContentDto.Representation.BIG -> MetaContentDto.Representation.BIG
                TokenMetaContentDto.Representation.ORIGINAL -> MetaContentDto.Representation.ORIGINAL
            },
            properties = null // TODO SOLANA: fill in properties if available.
        )
    }
}
