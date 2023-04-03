package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.CollectionDto
import com.rarible.protocol.solana.dto.CollectionsDto
import com.rarible.protocol.solana.dto.TokenMetaContentDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.continuation.page.Page

object SolanaCollectionConverter {

    fun convert(
        source: CollectionDto,
        blockchain: BlockchainDto,
    ): UnionCollection {
        return UnionCollection(
            id = CollectionIdDto(blockchain, source.address),
            parent = null,
            structureKind = UnionCollection.StructureKind.REGULAR,
            type = UnionCollection.Type.SOLANA,
            name = source.name,
            symbol = source.symbol,
            owner = source.owner?.let { UnionAddressConverter.convert(blockchain, it) },
            features = ArrayList<UnionCollection.Features>().apply {
                add(UnionCollection.Features.SECONDARY_SALE_FEES)
                addAll(source.features.map { convert(it) })
            },
            minters = source.creators?.map { UnionAddressConverter.convert(blockchain, it) } ?: emptyList(),
            meta = source.meta?.let { convert(it) }
        )
    }

    fun convert(page: CollectionsDto, blockchain: BlockchainDto): Page<UnionCollection> {
        return Page(
            total = 0,
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(feature: CollectionDto.Features): UnionCollection.Features {
        return when (feature) {
            CollectionDto.Features.APPROVE_FOR_ALL -> UnionCollection.Features.APPROVE_FOR_ALL
            CollectionDto.Features.BURN -> UnionCollection.Features.BURN
            CollectionDto.Features.MINT_AND_TRANSFER -> UnionCollection.Features.MINT_AND_TRANSFER
            CollectionDto.Features.MINT_WITH_ADDRESS -> UnionCollection.Features.MINT_WITH_ADDRESS
            CollectionDto.Features.SECONDARY_SALE_FEES -> UnionCollection.Features.SECONDARY_SALE_FEES
            CollectionDto.Features.SET_URI_PREFIX -> UnionCollection.Features.SET_URI_PREFIX
        }
    }

    private fun convert(meta: com.rarible.protocol.solana.dto.CollectionMetaDto): UnionCollectionMeta {
        return UnionCollectionMeta(
            name = meta.name,
            description = meta.description,
            content = meta.content.map {
                convert(it)
            }
        )
    }

    private fun convert(meta: TokenMetaContentDto): UnionMetaContent {
        return UnionMetaContent(
            url = meta.url,
            representation = MetaContentDto.Representation.valueOf(meta.representation.name),
            properties = UnionImageProperties(
                mimeType = meta.mimeType,
                size = meta.size
            )
        )
    }

}