package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.solana.protocol.dto.CollectionsDto
import com.rarible.solana.protocol.dto.TokenMetaContentDto

object SolanaCollectionConverter {

    fun convert(
        source: com.rarible.solana.protocol.dto.CollectionDto,
        blockchain: BlockchainDto
    ): CollectionDto {
        return CollectionDto(
            id = CollectionIdDto(blockchain, source.address),
            parent = null,
            blockchain = blockchain,
            type = CollectionDto.Type.SOLANA,
            name = source.name,
            symbol = source.symbol,
            owner = source.owner?.let { UnionAddressConverter.convert(blockchain, it) },
            features = source.features.map { convert(it) },
            minters = source.creators?.map { UnionAddressConverter.convert(blockchain, it) } ?: emptyList(),
            meta = source.meta?.let { convert(it) }
        )
    }

    fun convert(page: CollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = 0,
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(feature: com.rarible.solana.protocol.dto.CollectionDto.Features): CollectionDto.Features {
        return when (feature) {
            com.rarible.solana.protocol.dto.CollectionDto.Features.APPROVE_FOR_ALL -> CollectionDto.Features.APPROVE_FOR_ALL
            com.rarible.solana.protocol.dto.CollectionDto.Features.BURN -> CollectionDto.Features.BURN
            com.rarible.solana.protocol.dto.CollectionDto.Features.MINT_AND_TRANSFER -> CollectionDto.Features.MINT_AND_TRANSFER
            com.rarible.solana.protocol.dto.CollectionDto.Features.MINT_WITH_ADDRESS -> CollectionDto.Features.MINT_WITH_ADDRESS
            com.rarible.solana.protocol.dto.CollectionDto.Features.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
            com.rarible.solana.protocol.dto.CollectionDto.Features.SET_URI_PREFIX -> CollectionDto.Features.SET_URI_PREFIX
        }
    }

    private fun convert(meta: com.rarible.solana.protocol.dto.CollectionMetaDto): CollectionMetaDto {
        return CollectionMetaDto(
            name = meta.name,
            description = meta.description,
            content = meta.content.map {
                convert(it)
            }
        )
    }

    private fun convert(meta: TokenMetaContentDto): MetaContentDto {
        return ImageContentDto(
            url = meta.url,
            representation = MetaContentDto.Representation.valueOf(meta.representation.name),
            mimeType = meta.mimeType,
            size = meta.size,
        )
    }

}