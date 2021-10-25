package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftCollectionDto
import com.rarible.protocol.tezos.dto.NftCollectionFeatureDto
import com.rarible.protocol.tezos.dto.NftCollectionsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.UnionAddress

object TezosCollectionConverter {

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = UnionAddress(blockchain, source.id),
            name = source.name,
            symbol = source.symbol,
            owner = source.owner?.let { UnionAddress(blockchain, it) },
            type = CollectionDto.Type.TEZOS,
            features = source.features.map { convert(it) }
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(feature: NftCollectionFeatureDto): CollectionDto.Features {
        return when (feature) {
            NftCollectionFeatureDto.APPROVE_FOR_ALL -> CollectionDto.Features.APPROVE_FOR_ALL
            NftCollectionFeatureDto.BURN -> CollectionDto.Features.BURN
            NftCollectionFeatureDto.MINT_AND_TRANSFER -> CollectionDto.Features.MINT_AND_TRANSFER
            NftCollectionFeatureDto.MINT_WITH_ADDRESS -> CollectionDto.Features.MINT_WITH_ADDRESS
            NftCollectionFeatureDto.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
            NftCollectionFeatureDto.SET_URI_PREFIX -> CollectionDto.Features.SET_URI_PREFIX
        }
    }

}

