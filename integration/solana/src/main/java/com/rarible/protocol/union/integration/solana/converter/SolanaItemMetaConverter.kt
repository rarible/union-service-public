package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.solana.protocol.dto.TokenMetaAttributeDto
import com.rarible.solana.protocol.dto.TokenMetaContentDto
import com.rarible.solana.protocol.dto.TokenMetaDto

object SolanaItemMetaConverter {

    fun convert(tokenMeta: TokenMetaDto): UnionMeta =
        UnionMeta(
            name = tokenMeta.name,
            description = tokenMeta.description,
            attributes = tokenMeta.attributes.map { convert(it) },
            content = tokenMeta.content.map { convert(it) },
            restrictions = emptyList()
        )

    fun convert(source: TokenMetaAttributeDto): MetaAttributeDto {
        return MetaAttributeDto(
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
