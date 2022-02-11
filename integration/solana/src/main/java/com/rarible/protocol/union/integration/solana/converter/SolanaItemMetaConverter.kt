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
            attributes = tokenMeta.attributes.map { it.convert() },
            content = tokenMeta.content.map { it.convert() },
            restrictions = emptyList()
        )

    private fun TokenMetaAttributeDto.convert(): MetaAttributeDto =
        MetaAttributeDto(
            key = key,
            value = value,
            type = type,
            format = format
        )

    private fun TokenMetaContentDto.convert(): UnionMetaContent {
        return UnionMetaContent(
            url = url,
            representation = when (representation) {
                TokenMetaContentDto.Representation.PREVIEW -> MetaContentDto.Representation.PREVIEW
                TokenMetaContentDto.Representation.BIG -> MetaContentDto.Representation.BIG
                TokenMetaContentDto.Representation.ORIGINAL -> MetaContentDto.Representation.ORIGINAL
            },
            properties = null // TODO[Solana]: fill in properties if available.
        )
    }
}
